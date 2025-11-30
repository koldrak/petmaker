import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MascotaEscritorioIDE extends JFrame {

    private final DefaultTableModel modeloTablaAcciones;
    private final JTabbedPane tabsAcciones;
    private JComboBox<String> comboMascota;
    private JTextArea vistaScriptSeleccionado;
    private JLabel estadoLabel;

    private PetPreviewWindow petWindow;

    public MascotaEscritorioIDE() {
        setTitle("Editor de Mascotas de Escritorio - MVP");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 720);
        setLocationRelativeTo(null);

        setJMenuBar(crearMenu());

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(8, 8, 8, 8));

        JToolBar toolBar = crearBarraHerramientas();
        root.add(toolBar, BorderLayout.NORTH);

        JPanel panelPaleta = crearPanelPaletaBloques();
        root.add(panelPaleta, BorderLayout.WEST);

        JPanel panelCentro = new JPanel(new BorderLayout(6, 6));
        panelCentro.setBorder(BorderFactory.createTitledBorder("Scripts de la mascota"));
        tabsAcciones = new JTabbedPane();
        JPanel barraTabs = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnCrear = new JButton("Crear acción");
        JButton btnEliminar = new JButton("Eliminar acción");
        barraTabs.add(btnCrear);
        barraTabs.add(btnEliminar);
        panelCentro.add(barraTabs, BorderLayout.NORTH);
        panelCentro.add(tabsAcciones, BorderLayout.CENTER);
        root.add(panelCentro, BorderLayout.CENTER);

        JPanel panelDerecho = crearPanelDerecho();
        root.add(panelDerecho, BorderLayout.EAST);

        estadoLabel = new JLabel("Listo");
        estadoLabel.setBorder(new EmptyBorder(4, 4, 4, 4));
        root.add(estadoLabel, BorderLayout.SOUTH);

        // tabla de acciones para referencia (peso + uso)
        modeloTablaAcciones = new DefaultTableModel(new Object[]{"Usar", "Acción", "Peso"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class;
                if (columnIndex == 2) return Integer.class;
                return String.class;
            }
        };

        btnCrear.addActionListener(e -> crearNuevaAccion());
        btnEliminar.addActionListener(e -> eliminarAccion());

        add(root);
    }

    private JMenuBar crearMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu archivo = new JMenu("Archivo");
        JMenuItem nuevo = new JMenuItem("Nuevo proyecto");
        JMenuItem abrir = new JMenuItem("Abrir...");
        JMenuItem guardar = new JMenuItem("Guardar");
        JMenuItem salir = new JMenuItem("Salir");

        nuevo.addActionListener(e -> nuevoProyecto());
        abrir.addActionListener(e -> abrirProyecto());
        guardar.addActionListener(e -> guardarProyecto());
        salir.addActionListener(e -> System.exit(0));

        archivo.add(nuevo);
        archivo.add(abrir);
        archivo.add(guardar);
        archivo.addSeparator();
        archivo.add(salir);

        JMenu ayuda = new JMenu("Ayuda");
        JMenuItem tutorial = new JMenuItem("Tutorial rápido");
        tutorial.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "1) Arrastra bloques al lienzo.\n2) Doble clic para editar parámetros." +
                        "\n3) Crea varias acciones y presiona ▶ Probar.",
                "Tutorial", JOptionPane.INFORMATION_MESSAGE));
        ayuda.add(tutorial);

        menuBar.add(archivo);
        menuBar.add(ayuda);
        return menuBar;
    }

    private JToolBar crearBarraHerramientas() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(new JLabel("Mascota: "));
        comboMascota = new JComboBox<>(new String[]{"Gatito", "Slime", "Dragón", "Fantasma"});
        toolBar.add(comboMascota);
        toolBar.addSeparator();

        JButton btnProbar = new JButton("▶ Probar");
        JButton btnDetener = new JButton("⏹ Detener");
        JButton btnProbarEscritorio = new JButton("Probar en escritorio");
        toolBar.add(btnProbar);
        toolBar.add(btnDetener);
        toolBar.addSeparator();
        toolBar.add(btnProbarEscritorio);

        btnProbar.addActionListener(e -> lanzarVistaPrevia(false));
        btnProbarEscritorio.addActionListener(e -> lanzarVistaPrevia(true));
        btnDetener.addActionListener(e -> detenerMascota());
        return toolBar;
    }

    private JPanel crearPanelPaletaBloques() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(260, 0));
        panel.setBorder(BorderFactory.createTitledBorder("Paleta de bloques"));
        panel.setBackground(new Color(240, 248, 255));

        String[][] datosCategorias = new String[][]{
                {"Eventos", "cuando inicia la mascota", "cuando se hace clic en la mascota", "cuando se arrastra la mascota"},
                {"Movimiento", "mover", "seguir al mouse", "escapar del mouse", "teletransportar"},
                {"Animación", "reproducir animación", "agrandar mascota", "achicar mascota", "voltear horizontalmente", "voltear verticalmente"}
        };

        Map<String, String[]> bloquesPorCategoria = new LinkedHashMap<>();
        DefaultListModel<String> modeloCategorias = new DefaultListModel<>();
        DefaultListModel<String> modeloBloques = new DefaultListModel<>();

        for (String[] cat : datosCategorias) {
            modeloCategorias.addElement(cat[0]);
            String[] bloques = new String[cat.length - 1];
            System.arraycopy(cat, 1, bloques, 0, bloques.length);
            bloquesPorCategoria.put(cat[0], bloques);
        }

        JList<String> listaCategorias = new JList<>(modeloCategorias);
        listaCategorias.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaCategorias.setBorder(BorderFactory.createTitledBorder("Categoría"));

        JList<String> listaBloques = new JList<>(modeloBloques);
        listaBloques.setBorder(BorderFactory.createTitledBorder("Bloques"));
        listaBloques.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaBloques.setDragEnabled(true);
        listaBloques.setTransferHandler(new TransferHandler() {
            @Override
            protected Transferable createTransferable(JComponent c) {
                String val = listaBloques.getSelectedValue();
                return val == null ? null : new StringSelection(val);
            }

            @Override
            public int getSourceActions(JComponent c) {
                return COPY;
            }
        });

        listaCategorias.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String cat = listaCategorias.getSelectedValue();
                modeloBloques.clear();
                if (cat != null && bloquesPorCategoria.containsKey(cat)) {
                    for (String b : bloquesPorCategoria.get(cat)) {
                        modeloBloques.addElement(b);
                    }
                }
            }
        });

        if (!modeloCategorias.isEmpty()) {
            listaCategorias.setSelectedIndex(0);
        }

        JScrollPane scrollCat = new JScrollPane(listaCategorias);
        JScrollPane scrollBloq = new JScrollPane(listaBloques);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollCat, scrollBloq);
        split.setResizeWeight(0.35);
        panel.add(split, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearPanelDerecho() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(320, 0));
        panel.setBorder(BorderFactory.createTitledBorder("Vista previa y acciones aleatorias"));

        vistaScriptSeleccionado = new JTextArea();
        vistaScriptSeleccionado.setEditable(false);
        vistaScriptSeleccionado.setLineWrap(true);
        vistaScriptSeleccionado.setBorder(BorderFactory.createTitledBorder("Bloques de la acción"));

        JTable tabla = new JTable(modeloTablaAcciones);
        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setBorder(BorderFactory.createTitledBorder("Acciones disponibles"));

        panel.add(new JScrollPane(vistaScriptSeleccionado), BorderLayout.CENTER);
        panel.add(scrollTabla, BorderLayout.SOUTH);
        return panel;
    }

    private void crearNuevaAccion() {
        String nombre = JOptionPane.showInputDialog(this, "Nombre de la nueva acción:", "Nueva acción", JOptionPane.PLAIN_MESSAGE);
        if (nombre == null) return;
        nombre = nombre.trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre no puede estar vacío", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BlockCanvasPanel canvas = new BlockCanvasPanel();
        tabsAcciones.addTab(nombre, canvas);
        tabsAcciones.setSelectedComponent(canvas);
        modeloTablaAcciones.addRow(new Object[]{Boolean.TRUE, nombre, Integer.valueOf(1)});
        actualizarVistaBloques();
    }

    private void eliminarAccion() {
        int idx = tabsAcciones.getSelectedIndex();
        if (idx == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona una acción para eliminar", "Eliminar", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String nombre = tabsAcciones.getTitleAt(idx);
        tabsAcciones.removeTabAt(idx);
        for (int i = 0; i < modeloTablaAcciones.getRowCount(); i++) {
            if (nombre.equals(modeloTablaAcciones.getValueAt(i, 1))) {
                modeloTablaAcciones.removeRow(i);
                break;
            }
        }
        actualizarVistaBloques();
    }

    private void actualizarVistaBloques() {
        BlockCanvasPanel canvas = getCanvasSeleccionado();
        if (canvas == null) {
            vistaScriptSeleccionado.setText("Sin acción seleccionada");
            return;
        }
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (BlockNodeData data : canvas.exportData()) {
            sb.append(i++).append(". ").append(data.baseName);
            if (!data.paramSummary.isEmpty()) {
                sb.append(" [").append(data.paramSummary).append("]");
            }
            sb.append('\n');
        }
        vistaScriptSeleccionado.setText(sb.toString());
    }

    private BlockCanvasPanel getCanvasSeleccionado() {
        Component c = tabsAcciones.getSelectedComponent();
        if (c instanceof BlockCanvasPanel) {
            return (BlockCanvasPanel) c;
        }
        return null;
    }

    private void lanzarVistaPrevia(boolean flotante) {
        BlockCanvasPanel canvas = getCanvasSeleccionado();
        if (canvas == null || canvas.exportData().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Necesitas al menos un bloque en la acción seleccionada.",
                    "Sin acción", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (petWindow != null) {
            petWindow.dispose();
        }

        petWindow = new PetPreviewWindow(flotante);
        petWindow.setVisible(true);
        petWindow.runScript(canvas.exportData());
        estadoLabel.setText("Ejecutando " + tabsAcciones.getTitleAt(tabsAcciones.getSelectedIndex()) +
                (flotante ? " en escritorio" : " en ventana"));
    }

    private void detenerMascota() {
        if (petWindow != null) {
            petWindow.dispose();
            petWindow = null;
        }
        estadoLabel.setText("Listo");
    }

    private void nuevoProyecto() {
        tabsAcciones.removeAll();
        modeloTablaAcciones.setRowCount(0);
        vistaScriptSeleccionado.setText("");
        detenerMascota();
    }

    private void guardarProyecto() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar proyecto");
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                bw.write("PET=" + comboMascota.getSelectedItem() + "\n");
                for (int i = 0; i < tabsAcciones.getTabCount(); i++) {
                    String nombre = tabsAcciones.getTitleAt(i);
                    bw.write("SCRIPT|" + nombre + "\n");
                    BlockCanvasPanel canvas = (BlockCanvasPanel) tabsAcciones.getComponentAt(i);
                    for (BlockNodeData data : canvas.exportData()) {
                        bw.write(String.format("BLOCK|%s|%s|%d|%d|%d|%d|%d\n",
                                escape(data.baseName), escape(data.paramSummary), data.x, data.y,
                                data.width, data.height, data.fillColor.getRGB()));
                    }
                }
                estadoLabel.setText("Proyecto guardado");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "No se pudo guardar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void abrirProyecto() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Abrir proyecto");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                nuevoProyecto();
                String line;
                BlockCanvasPanel actual = null;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("PET=")) {
                        comboMascota.setSelectedItem(line.substring(4));
                    } else if (line.startsWith("SCRIPT|")) {
                        String nombre = line.substring(7);
                        actual = new BlockCanvasPanel();
                        tabsAcciones.addTab(nombre, actual);
                        modeloTablaAcciones.addRow(new Object[]{Boolean.TRUE, nombre, Integer.valueOf(1)});
                    } else if (line.startsWith("BLOCK|") && actual != null) {
                        String[] parts = line.split("\\|", -1);
                        if (parts.length >= 8) {
                            BlockNodeData data = new BlockNodeData(
                                    unescape(parts[1]),
                                    unescape(parts[2]),
                                    Integer.parseInt(parts[3]),
                                    Integer.parseInt(parts[4]),
                                    Integer.parseInt(parts[5]),
                                    Integer.parseInt(parts[6]),
                                    new Color(Integer.parseInt(parts[7]))
                            );
                            actual.importData(data);
                        }
                    }
                }
                actualizarVistaBloques();
                estadoLabel.setText("Proyecto cargado");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "No se pudo abrir: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String escape(String text) {
        return text.replace("\\", "\\\\").replace("|", "\\|");
    }

    private String unescape(String text) {
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (char c : text.toCharArray()) {
            if (escape) {
                sb.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ------------------ Launch ------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MascotaEscritorioIDE().setVisible(true));
    }

    // ------------------ Clases internas ------------------
    private static class BlockNodeData {
        final String baseName;
        final String paramSummary;
        final int x, y, width, height;
        final Color fillColor;

        BlockNodeData(String baseName, String paramSummary, int x, int y, int width, int height, Color fillColor) {
            this.baseName = baseName;
            this.paramSummary = paramSummary == null ? "" : paramSummary;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.fillColor = fillColor;
        }
    }

    private static class BlockCanvasPanel extends JPanel {
        private final List<BlockNodeData> blocks = new ArrayList<>();
        private BlockNodeData dragging;
        private int offsetX;
        private int offsetY;

        BlockCanvasPanel() {
            setBackground(new Color(250, 250, 250));
            setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
            setPreferredSize(new Dimension(520, 460));
            setTransferHandler(new BlockDropTransferHandler(this));

            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    dragging = findBlock(e.getX(), e.getY());
                    if (dragging != null) {
                        offsetX = e.getX() - dragging.x;
                        offsetY = e.getY() - dragging.y;
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragging != null) {
                        int idx = blocks.indexOf(dragging);
                        blocks.set(idx, new BlockNodeData(dragging.baseName, dragging.paramSummary,
                                e.getX() - offsetX, e.getY() - offsetY, dragging.width, dragging.height, dragging.fillColor));
                        dragging = blocks.get(idx);
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    dragging = null;
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        BlockNodeData n = findBlock(e.getX(), e.getY());
                        if (n != null) {
                            BlockEditDialog dialog = new BlockEditDialog(SwingUtilities.getWindowAncestor(BlockCanvasPanel.this), n);
                            dialog.setVisible(true);
                            if (dialog.getParamSummary() != null) {
                                int idx = blocks.indexOf(n);
                                blocks.set(idx, new BlockNodeData(n.baseName, dialog.getParamSummary(), n.x, n.y, n.width, n.height, n.fillColor));
                                repaint();
                            }
                        }
                    }
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        void addBlock(String name) {
            int baseX = 30;
            int baseY = 20 + blocks.size() * 60;
            Color color = pickColor(name);
            blocks.add(new BlockNodeData(name, "", baseX, baseY, 240, 42, color));
            repaint();
        }

        void importData(BlockNodeData data) {
            blocks.add(data);
            repaint();
        }

        List<BlockNodeData> exportData() {
            return new ArrayList<>(blocks);
        }

        private Color pickColor(String name) {
            if (name.startsWith("cuando")) {
                return new Color(255, 220, 160);
            }
            if (name.equals("mover") || name.equals("seguir al mouse") || name.equals("escapar del mouse") || name.equals("teletransportar")) {
                return new Color(180, 220, 255);
            }
            return new Color(210, 190, 255);
        }

        private BlockNodeData findBlock(int x, int y) {
            for (int i = blocks.size() - 1; i >= 0; i--) {
                BlockNodeData n = blocks.get(i);
                if (x >= n.x && x <= n.x + n.width && y >= n.y && y <= n.y + n.height) {
                    return n;
                }
            }
            return null;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(150, 150, 150));
            List<BlockNodeData> sorted = new ArrayList<>(blocks);
            sorted.sort((a, b) -> Integer.compare(a.y, b.y));
            for (int i = 0; i < sorted.size() - 1; i++) {
                BlockNodeData a = sorted.get(i);
                BlockNodeData b = sorted.get(i + 1);
                g.drawLine(a.x + a.width / 2, a.y + a.height, b.x + b.width / 2, b.y);
            }
            for (BlockNodeData n : blocks) {
                g.setColor(n.fillColor);
                g.fillRoundRect(n.x, n.y, n.width, n.height, 12, 12);
                g.setColor(Color.DARK_GRAY);
                g.drawRoundRect(n.x, n.y, n.width, n.height, 12, 12);
                g.setColor(Color.BLACK);
                String text = n.baseName + (n.paramSummary.isEmpty() ? "" : " [" + n.paramSummary + "]");
                FontMetrics fm = g.getFontMetrics();
                int ty = n.y + (n.height + fm.getAscent() - fm.getDescent()) / 2;
                g.drawString(text, n.x + 8, ty);
            }
        }
    }

    private static class BlockDropTransferHandler extends TransferHandler {
        private final BlockCanvasPanel canvas;

        BlockDropTransferHandler(BlockCanvasPanel canvas) {
            this.canvas = canvas;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) return false;
            try {
                Transferable t = support.getTransferable();
                String blockName = (String) t.getTransferData(DataFlavor.stringFlavor);
                canvas.addBlock(blockName);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    private static class BlockEditDialog extends JDialog {
        private String paramSummary;
        private final String blockName;

        BlockEditDialog(Window owner, BlockNodeData data) {
            super(owner, "Editar bloque: " + data.baseName, ModalityType.APPLICATION_MODAL);
            this.blockName = data.baseName;
            this.paramSummary = data.paramSummary;
            initUI();
        }

        private void initUI() {
            setLayout(new BorderLayout(8, 8));
            JPanel centro = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.anchor = GridBagConstraints.WEST;

            List<JTextField> campos = new ArrayList<>();
            List<String> nombres = new ArrayList<>();

            if ("mover".equals(blockName)) {
                JLabel l1 = new JLabel("Dirección (0-360):");
                JTextField t1 = new JTextField(8);
                JLabel l2 = new JLabel("Velocidad (px/frame):");
                JTextField t2 = new JTextField(8);
                centro.add(l1, gbc);
                gbc.gridx = 1; centro.add(t1, gbc);
                gbc.gridx = 0; gbc.gridy++;
                centro.add(l2, gbc);
                gbc.gridx = 1; centro.add(t2, gbc);
                campos.add(t1); nombres.add("dir");
                campos.add(t2); nombres.add("vel");
            } else if ("reproducir animación".equals(blockName)) {
                JLabel l1 = new JLabel("Nombre animación:");
                JTextField t1 = new JTextField(12);
                centro.add(l1, gbc);
                gbc.gridx = 1; centro.add(t1, gbc);
                campos.add(t1); nombres.add("anim");
            }

            add(centro, BorderLayout.CENTER);

            JPanel sur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton ok = new JButton("Guardar");
            JButton cancel = new JButton("Cancelar");
            sur.add(ok);
            sur.add(cancel);
            add(sur, BorderLayout.SOUTH);

            ok.addActionListener(e -> {
                if (!campos.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < campos.size(); i++) {
                        String val = campos.get(i).getText().trim();
                        if (!val.isEmpty()) {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append(nombres.get(i)).append("=").append(val);
                        }
                    }
                    paramSummary = sb.toString();
                }
                dispose();
            });
            cancel.addActionListener(e -> dispose());
            pack();
            setLocationRelativeTo(getOwner());
        }

        public String getParamSummary() {
            return paramSummary;
        }
    }

    private static class PetPreviewWindow extends JWindow {
        private final Timer timer;
        private int x = 80;
        private int y = 80;
        private int size = 120;
        private double velX = 0;
        private double velY = 0;
        private final Random random = new Random();
        private List<BlockNodeData> actions = new ArrayList<>();
        private Point mouse = new Point(0, 0);

        PetPreviewWindow(boolean transparent) {
            if (transparent) {
                setBackground(new Color(0, 0, 0, 0));
                setAlwaysOnTop(true);
            }
            setSize(260, 260);
            setLocation(200, 200);
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    mouse = e.getLocationOnScreen();
                }
            });

            timer = new Timer(30, e -> tick());
        }

        void runScript(List<BlockNodeData> data) {
            this.actions = data;
            parseActions();
            timer.start();
        }

        private void parseActions() {
            velX = velY = 0;
            size = 120;
            for (BlockNodeData node : actions) {
                switch (node.baseName) {
                    case "mover":
                        double dir = parseParam(node.paramSummary, "dir", 90);
                        double vel = parseParam(node.paramSummary, "vel", 3);
                        double rad = Math.toRadians(dir);
                        velX += Math.cos(rad) * vel;
                        velY += Math.sin(rad) * vel;
                        break;
                    case "agrandar mascota":
                        size += 20;
                        break;
                    case "achicar mascota":
                        size = Math.max(60, size - 20);
                        break;
                    default:
                        break;
                }
            }
        }

        private double parseParam(String paramSummary, String key, double def) {
            if (paramSummary == null) return def;
            String[] parts = paramSummary.split(",");
            for (String p : parts) {
                String[] kv = p.trim().split("=");
                if (kv.length == 2 && kv[0].trim().equalsIgnoreCase(key)) {
                    try { return Double.parseDouble(kv[1].trim()); } catch (NumberFormatException ignored) {}
                }
            }
            return def;
        }

        private void tick() {
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            for (BlockNodeData node : actions) {
                switch (node.baseName) {
                    case "seguir al mouse":
                        Point p = MouseInfo.getPointerInfo().getLocation();
                        double dx = p.x - x;
                        double dy = p.y - y;
                        double len = Math.max(1, Math.hypot(dx, dy));
                        velX = dx / len * 5;
                        velY = dy / len * 5;
                        break;
                    case "escapar del mouse":
                        Point pm = MouseInfo.getPointerInfo().getLocation();
                        double ex = x - pm.x;
                        double ey = y - pm.y;
                        double elen = Math.max(1, Math.hypot(ex, ey));
                        velX = ex / elen * 5;
                        velY = ey / elen * 5;
                        break;
                    case "teletransportar":
                        x = random.nextInt(Math.max(100, screen.width - size));
                        y = random.nextInt(Math.max(100, screen.height - size));
                        break;
                    default:
                        break;
                }
            }
            x += velX;
            y += velY;

            if (x < 0 || x + size > screen.width) velX *= -1;
            if (y < 0 || y + size > screen.height) velY *= -1;
            x = Math.max(0, Math.min(x, screen.width - size));
            y = Math.max(0, Math.min(y, screen.height - size));

            setLocation(x, y);
            repaint();
        }

        @Override
        public void dispose() {
            timer.stop();
            super.dispose();
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(255, 230, 180));
            g2.fillOval(10, 10, size, size);
            g2.setColor(Color.DARK_GRAY);
            g2.drawOval(10, 10, size, size);
            int eye = size / 10;
            int eyeY = 30 + size / 3;
            int eyeOffset = size / 4;
            g2.fillOval(10 + eyeOffset, eyeY, eye, eye);
            g2.fillOval(10 + size - eyeOffset - eye, eyeY, eye, eye);
            int mouthWidth = size / 2;
            int mouthX = 10 + (size - mouthWidth) / 2;
            int mouthY = 10 + size / 2 + size / 6;
            g2.drawArc(mouthX, mouthY, mouthWidth, size / 4, 0, -180);
            g2.dispose();
        }
    }
}
