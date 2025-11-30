import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.JColorChooser;
import javax.swing.JTable;
import java.awt.FontMetrics;
import java.util.Collections;
import java.util.function.Supplier;

public class MascotaEscritorioIDE extends JFrame {

    private final DefaultTableModel modeloTablaAcciones;
    private final JTabbedPane tabsAcciones;
    private JComboBox<String> comboMascota;
    private JTextArea vistaScriptSeleccionado;
    private JLabel estadoLabel;
    private final List<ScriptSprite> scriptsCatalog = new ArrayList<>();
    private DefaultListModel<String> modeloListaScripts;
    private JList<String> listaScripts;
    private PreviewMascotaPanel canvasPreview;

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

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column == 2;
            }
        };

        btnCrear.addActionListener(e -> crearNuevaAccion());
        btnEliminar.addActionListener(e -> eliminarAccion());

        add(root);
    }

    private BlockCanvasPanel crearLienzoAccion() {
        return new BlockCanvasPanel(this::obtenerNombresScripts);
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

        // ---- Zona inferior: variables ----
        DefaultListModel<String> modeloVariables = new DefaultListModel<>();
        JList<String> listaVariables = new JList<>(modeloVariables);
        listaVariables.setBorder(BorderFactory.createTitledBorder("Variables"));
        JScrollPane scrollVariables = new JScrollPane(listaVariables);
        scrollVariables.setPreferredSize(new Dimension(0, 120));

        JButton btnCrearVariable = new JButton("Crear variable");
        btnCrearVariable.addActionListener(e -> {
            String nombre = JOptionPane.showInputDialog(panel, "Nombre de la nueva variable:", "Crear variable", JOptionPane.PLAIN_MESSAGE);
            if (nombre == null) return;
            nombre = nombre.trim();
            if (nombre.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "El nombre de la variable no puede estar vacío.", "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            for (int i = 0; i < modeloVariables.size(); i++) {
                if (modeloVariables.getElementAt(i).equals(nombre)) {
                    JOptionPane.showMessageDialog(panel, "Ya existe una variable con ese nombre.", "Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            modeloVariables.addElement(nombre);
        });

        JPanel panelVariables = new JPanel(new BorderLayout());
        panelVariables.add(btnCrearVariable, BorderLayout.NORTH);
        panelVariables.add(scrollVariables, BorderLayout.CENTER);

        JPanel contenedorCentro = new JPanel(new BorderLayout());
        contenedorCentro.add(split, BorderLayout.CENTER);
        contenedorCentro.add(panelVariables, BorderLayout.SOUTH);

        panel.add(contenedorCentro, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearPanelDerecho() {
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setPreferredSize(new Dimension(360, 0));
        contenedor.setBorder(BorderFactory.createEmptyBorder());

        JPanel panelPreview = crearPanelPreview();
        JPanel panelAnimaciones = crearPanelAnimaciones();

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelPreview, panelAnimaciones);
        split.setResizeWeight(0.55);

        contenedor.add(split, BorderLayout.CENTER);
        return contenedor;
    }

    private JPanel crearPanelPreview() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Vista previa"));

        modeloListaScripts = new DefaultListModel<>();
        listaScripts = new JList<>(modeloListaScripts);
        JScrollPane scrollLista = new JScrollPane(listaScripts);
        scrollLista.setBorder(BorderFactory.createTitledBorder("Scripts creados"));

        canvasPreview = new PreviewMascotaPanel();
        canvasPreview.setPreferredSize(new Dimension(300, 240));

        vistaScriptSeleccionado = new JTextArea();
        vistaScriptSeleccionado.setEditable(false);
        vistaScriptSeleccionado.setLineWrap(true);
        vistaScriptSeleccionado.setBorder(BorderFactory.createTitledBorder("Bloques de la acción"));
        JScrollPane scrollVista = new JScrollPane(vistaScriptSeleccionado);
        scrollVista.setPreferredSize(new Dimension(180, 0));

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnCrearScript = new JButton("Crear script");
        JButton btnEliminarScript = new JButton("Eliminar script");
        barra.add(btnCrearScript);
        barra.add(btnEliminarScript);

        JPanel centro = new JPanel(new BorderLayout());
        centro.add(canvasPreview, BorderLayout.CENTER);
        centro.add(scrollVista, BorderLayout.EAST);

        panel.add(barra, BorderLayout.NORTH);
        panel.add(centro, BorderLayout.CENTER);
        panel.add(scrollLista, BorderLayout.SOUTH);

        btnCrearScript.addActionListener(e -> {
            ScriptEditorDialog dialog = new ScriptEditorDialog(this);
            dialog.setVisible(true);
            ScriptSprite nuevo = dialog.getResult();
            if (nuevo != null) {
                scriptsCatalog.add(nuevo);
                modeloListaScripts.addElement(nuevo.getNombre());
                listaScripts.setSelectedIndex(modeloListaScripts.size() - 1);
                canvasPreview.setPixelData(nuevo.getPixels());
                canvasPreview.repaint();
            }
        });

        btnEliminarScript.addActionListener(e -> {
            int idx = listaScripts.getSelectedIndex();
            if (idx == -1) {
                JOptionPane.showMessageDialog(panel, "Selecciona un script de la lista.", "Eliminar script", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            scriptsCatalog.remove(idx);
            modeloListaScripts.remove(idx);
            if (!scriptsCatalog.isEmpty()) {
                int nuevoIdx = Math.min(idx, scriptsCatalog.size() - 1);
                listaScripts.setSelectedIndex(nuevoIdx);
                canvasPreview.setPixelData(scriptsCatalog.get(nuevoIdx).getPixels());
            } else {
                canvasPreview.setPixelData(null);
            }
            canvasPreview.repaint();
        });

        listaScripts.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int idx = listaScripts.getSelectedIndex();
                if (idx >= 0 && idx < scriptsCatalog.size()) {
                    canvasPreview.setPixelData(scriptsCatalog.get(idx).getPixels());
                } else {
                    canvasPreview.setPixelData(null);
                }
                canvasPreview.repaint();
            }
        });

        return panel;
    }

    private JPanel crearPanelAnimaciones() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Acciones aleatorias"));

        JTable tabla = new JTable(modeloTablaAcciones);
        JScrollPane scroll = new JScrollPane(tabla);

        JPanel ayuda = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 4, 4);

        JLabel texto = new JLabel("<html>Aquí se mostrarán las acciones creadas.<br>Marca cuáles entran en el pool aleatorio y ajusta el peso.</html>");
        texto.setFont(texto.getFont().deriveFont(Font.PLAIN, 11f));
        ayuda.add(texto, gbc);

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(ayuda, BorderLayout.SOUTH);
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

        BlockCanvasPanel canvas = crearLienzoAccion();
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

    private List<String> obtenerNombresScripts() {
        List<String> nombres = new ArrayList<>();
        for (ScriptSprite s : scriptsCatalog) {
            nombres.add(s.getNombre());
        }
        return nombres;
    }

    private BlockCanvasPanel getCanvasSeleccionado() {
        Component c = tabsAcciones.getSelectedComponent();
        if (c instanceof BlockCanvasPanel) {
            return (BlockCanvasPanel) c;
        }
        return null;
    }

    private BlockCanvasPanel buscarCanvasPorNombre(String nombre) {
        for (int i = 0; i < tabsAcciones.getTabCount(); i++) {
            if (tabsAcciones.getTitleAt(i).equals(nombre)) {
                Component c = tabsAcciones.getComponentAt(i);
                if (c instanceof BlockCanvasPanel) {
                    return (BlockCanvasPanel) c;
                }
            }
        }
        return null;
    }

    private List<BlockNodeData> clonarBloques(List<BlockNodeData> origen) {
        List<BlockNodeData> copia = new ArrayList<>();
        for (BlockNodeData d : origen) {
            copia.add(new BlockNodeData(d.baseName, d.paramSummary, d.x, d.y, d.width, d.height, d.fillColor));
        }
        return copia;
    }

    private List<RandomActionEntry> construirPoolAleatorio() {
        List<RandomActionEntry> pool = new ArrayList<>();
        for (int i = 0; i < modeloTablaAcciones.getRowCount(); i++) {
            Boolean usar = (Boolean) modeloTablaAcciones.getValueAt(i, 0);
            String nombre = (String) modeloTablaAcciones.getValueAt(i, 1);
            Integer peso = (Integer) modeloTablaAcciones.getValueAt(i, 2);
            if (usar != null && usar && nombre != null) {
                BlockCanvasPanel canvas = buscarCanvasPorNombre(nombre);
                if (canvas != null && !canvas.exportData().isEmpty()) {
                    pool.add(new RandomActionEntry(nombre, peso == null ? 1 : Math.max(1, peso), clonarBloques(canvas.exportData())));
                }
            }
        }
        return pool;
    }

    private void lanzarVistaPrevia(boolean flotante) {
        BlockCanvasPanel canvas = getCanvasSeleccionado();
        List<BlockNodeData> accionPrincipal = canvas == null ? Collections.emptyList() : canvas.exportData();
        List<RandomActionEntry> pool = construirPoolAleatorio();

        if ((accionPrincipal == null || accionPrincipal.isEmpty()) && pool.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Necesitas al menos un bloque en la acción seleccionada o acciones aleatorias activas.",
                    "Sin acción", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (petWindow != null) {
            petWindow.dispose();
        }

        petWindow = new PetPreviewWindow(flotante);
        petWindow.setVisible(true);
        petWindow.runScript(accionPrincipal, pool);
        String nombre = canvas != null ? tabsAcciones.getTitleAt(tabsAcciones.getSelectedIndex()) : "pool aleatorio";
        estadoLabel.setText("Ejecutando " + nombre +
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
        scriptsCatalog.clear();
        if (modeloListaScripts != null) {
            modeloListaScripts.clear();
        }
        if (canvasPreview != null) {
            canvasPreview.setPixelData(null);
            canvasPreview.repaint();
        }
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
                        actual = crearLienzoAccion();
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

    private static class RandomActionEntry {
        final String nombre;
        final int peso;
        final List<BlockNodeData> bloques;

        RandomActionEntry(String nombre, int peso, List<BlockNodeData> bloques) {
            this.nombre = nombre;
            this.peso = peso;
            this.bloques = bloques;
        }
    }

    private static class ScriptSprite {
        private final String nombre;
        private final Color[][] pixels;

        ScriptSprite(String nombre, Color[][] pixels) {
            this.nombre = nombre;
            this.pixels = pixels;
        }

        String getNombre() {
            return nombre;
        }

        Color[][] getPixels() {
            return pixels;
        }
    }

    private static class ScriptEditorDialog extends JDialog {
        private JTextField txtNombre;
        private PixelGridPanel gridPanel;
        private Color currentColor = Color.BLACK;
        private ScriptSprite result;

        ScriptEditorDialog(JFrame owner) {
            super(owner, "Nuevo script", true);
            initUI();
        }

        private void initUI() {
            setLayout(new BorderLayout(8, 8));

            JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT));
            norte.add(new JLabel("Nombre del script:"));
            txtNombre = new JTextField(20);
            norte.add(txtNombre);
            add(norte, BorderLayout.NORTH);

            gridPanel = new PixelGridPanel(128, 128, 4);
            gridPanel.setCurrentColor(currentColor);
            JScrollPane scroll = new JScrollPane(gridPanel);
            add(scroll, BorderLayout.CENTER);

            JPanel sur = new JPanel(new BorderLayout());

            JPanel panelColor = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel lblColorActual = new JLabel("Color actual:");
            JPanel muestraColor = new JPanel();
            muestraColor.setPreferredSize(new Dimension(32, 32));
            muestraColor.setBackground(currentColor);

            JButton btnElegirColor = new JButton("Elegir color...");
            JButton btnTransparente = new JButton("Borrador (transparente)");

            panelColor.add(lblColorActual);
            panelColor.add(muestraColor);
            panelColor.add(btnElegirColor);
            panelColor.add(btnTransparente);

            sur.add(panelColor, BorderLayout.NORTH);

            JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnGuardar = new JButton("Guardar");
            JButton btnCancelar = new JButton("Cancelar");
            panelBotones.add(btnGuardar);
            panelBotones.add(btnCancelar);
            sur.add(panelBotones, BorderLayout.SOUTH);

            add(sur, BorderLayout.SOUTH);

            btnElegirColor.addActionListener(e -> {
                Color elegido = JColorChooser.showDialog(this, "Selecciona un color", currentColor);
                if (elegido != null) {
                    currentColor = elegido;
                    muestraColor.setBackground(currentColor);
                    gridPanel.setCurrentColor(currentColor);
                }
            });

            btnTransparente.addActionListener(e -> {
                currentColor = null;
                muestraColor.setBackground(new Color(220, 220, 220));
                gridPanel.setCurrentColor(null);
            });

            btnGuardar.addActionListener(e -> {
                String nombre = txtNombre.getText().trim();
                if (nombre.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Debes ingresar un nombre para el script.", "Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                result = new ScriptSprite(nombre, gridPanel.getPixelsCopy());
                dispose();
            });
            btnCancelar.addActionListener(e -> {
                result = null;
                dispose();
            });

            pack();
            setSize(620, 520);
            setLocationRelativeTo(getOwner());
        }

        ScriptSprite getResult() {
            return result;
        }
    }

    private static class PixelGridPanel extends JPanel {
        private final int rows;
        private final int cols;
        private final int cellSize;
        private final Color[][] pixels;
        private Color currentColor;

        PixelGridPanel(int rows, int cols, int cellSize) {
            this.rows = rows;
            this.cols = cols;
            this.cellSize = cellSize;
            this.pixels = new Color[rows][cols];
            setPreferredSize(new Dimension(cols * cellSize, rows * cellSize));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

            MouseAdapter mouse = new MouseAdapter() {
                private boolean painting = false;

                @Override
                public void mousePressed(MouseEvent e) {
                    painting = true;
                    apply(e.getX(), e.getY());
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    painting = false;
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (painting) apply(e.getX(), e.getY());
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        void setCurrentColor(Color color) {
            this.currentColor = color;
        }

        private void apply(int x, int y) {
            int c = x / cellSize;
            int r = y / cellSize;
            if (r >= 0 && r < rows && c >= 0 && c < cols) {
                pixels[r][c] = currentColor;
                repaint();
            }
        }

        Color[][] getPixelsCopy() {
            Color[][] copy = new Color[rows][cols];
            for (int r = 0; r < rows; r++) {
                System.arraycopy(pixels[r], 0, copy[r], 0, cols);
            }
            return copy;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int x = c * cellSize;
                    int y = r * cellSize;
                    Color col = pixels[r][c];
                    if (col != null) {
                        g.setColor(col);
                        g.fillRect(x, y, cellSize, cellSize);
                    }
                    g.setColor(new Color(230, 230, 230));
                    g.drawRect(x, y, cellSize, cellSize);
                }
            }
        }
    }

    private static class PreviewMascotaPanel extends JPanel {
        private Color[][] pixels;

        PreviewMascotaPanel() {
            setBackground(new Color(250, 250, 250));
            setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        }

        void setPixelData(Color[][] pixels) {
            this.pixels = pixels;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (pixels == null) {
                int size = 120;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                g.setColor(new Color(255, 230, 180));
                g.fillOval(x, y, size, size);
                g.setColor(Color.DARK_GRAY);
                g.drawOval(x, y, size, size);
                int eyeRadius = size / 10;
                int eyeY = y + size / 3;
                int eyeXOffset = size / 4;
                g.fillOval(x + eyeXOffset - eyeRadius / 2, eyeY, eyeRadius, eyeRadius);
                g.fillOval(x + size - eyeXOffset - eyeRadius / 2, eyeY, eyeRadius, eyeRadius);
                int mouthWidth = size / 2;
                int mouthX = x + (size - mouthWidth) / 2;
                int mouthY = y + size / 2 + size / 6;
                g.drawArc(mouthX, mouthY, mouthWidth, size / 4, 0, -180);
            } else {
                int cellSize = Math.max(2, Math.min(getWidth() / Math.max(1, pixels[0].length), getHeight() / Math.max(1, pixels.length)));
                for (int r = 0; r < pixels.length; r++) {
                    for (int c = 0; c < pixels[r].length; c++) {
                        Color col = pixels[r][c];
                        if (col != null) {
                            g.setColor(col);
                            g.fillRect(c * cellSize, r * cellSize, cellSize, cellSize);
                        }
                    }
                }
            }
        }
    }

    private static class BlockCanvasPanel extends JPanel {
        private final List<BlockNodeData> blocks = new ArrayList<>();
        private BlockNodeData dragging;
        private int offsetX;
        private int offsetY;
        private final Supplier<List<String>> scriptNamesSupplier;

        BlockCanvasPanel(Supplier<List<String>> scriptNamesSupplier) {
            this.scriptNamesSupplier = scriptNamesSupplier;
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
                            List<String> scriptsDisponibles = scriptNamesSupplier == null ? Collections.emptyList() : scriptNamesSupplier.get();
                            BlockEditDialog dialog = new BlockEditDialog(SwingUtilities.getWindowAncestor(BlockCanvasPanel.this), n, scriptsDisponibles);
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
        private final List<String> scriptsDisponibles;

        BlockEditDialog(Window owner, BlockNodeData data, List<String> scriptsDisponibles) {
            super(owner, "Editar bloque: " + data.baseName, ModalityType.APPLICATION_MODAL);
            this.blockName = data.baseName;
            this.paramSummary = data.paramSummary;
            this.scriptsDisponibles = scriptsDisponibles;
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

            JPanel sur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton ok = new JButton("Guardar");
            JButton cancel = new JButton("Cancelar");
            sur.add(ok);
            sur.add(cancel);
            add(sur, BorderLayout.SOUTH);

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
                gbc.gridwidth = 2;
                JLabel lTitulo = new JLabel("Selecciona scripts y orden:");
                centro.add(lTitulo, gbc);
                gbc.gridy++;

                DefaultListModel<String> modeloDisponibles = new DefaultListModel<>();
                for (String s : scriptsDisponibles) modeloDisponibles.addElement(s);
                JList<String> listaDisponibles = new JList<>(modeloDisponibles);
                listaDisponibles.setBorder(BorderFactory.createTitledBorder("Scripts"));

                DefaultListModel<String> modeloSecuencia = new DefaultListModel<>();
                String ordenPrevio = extraerValor(paramSummary, "orden");
                if (ordenPrevio != null) {
                    for (String s : ordenPrevio.split(">")) {
                        if (!s.trim().isEmpty()) modeloSecuencia.addElement(s.trim());
                    }
                }
                JList<String> listaSecuencia = new JList<>(modeloSecuencia);
                listaSecuencia.setBorder(BorderFactory.createTitledBorder("Orden"));

                JPanel controles = new JPanel();
                controles.setLayout(new BoxLayout(controles, BoxLayout.Y_AXIS));
                JButton btnAgregar = new JButton("Añadir >");
                JButton btnRemover = new JButton("< Quitar");
                JButton btnSubir = new JButton("Subir");
                JButton btnBajar = new JButton("Bajar");
                controles.add(btnAgregar);
                controles.add(btnRemover);
                controles.add(Box.createVerticalStrut(8));
                controles.add(btnSubir);
                controles.add(btnBajar);

                gbc.gridwidth = 1;
                gbc.gridx = 0; centro.add(new JScrollPane(listaDisponibles), gbc);
                gbc.gridx = 1; centro.add(controles, gbc);
                gbc.gridx = 2; centro.add(new JScrollPane(listaSecuencia), gbc);
                gbc.gridy++;
                gbc.gridx = 0;
                gbc.gridwidth = 3;

                JPanel tiempos = new JPanel(new GridLayout(2, 2, 6, 4));
                tiempos.setBorder(BorderFactory.createTitledBorder("Intervalos"));
                JTextField txtIntervalo = new JTextField(8);
                JTextField txtDuracion = new JTextField(8);
                txtIntervalo.setText(defaultOrParam(paramSummary, "intervaloMs", "500"));
                txtDuracion.setText(defaultOrParam(paramSummary, "duracionMs", "4000"));
                tiempos.add(new JLabel("Intervalo ms:"));
                tiempos.add(txtIntervalo);
                tiempos.add(new JLabel("Duración bucle ms:"));
                tiempos.add(txtDuracion);
                centro.add(tiempos, gbc);

                add(centro, BorderLayout.CENTER);

                btnAgregar.addActionListener(e -> {
                    for (String sel : listaDisponibles.getSelectedValuesList()) {
                        modeloSecuencia.addElement(sel);
                    }
                });
                btnRemover.addActionListener(e -> {
                    for (String sel : listaSecuencia.getSelectedValuesList()) {
                        modeloSecuencia.removeElement(sel);
                    }
                });
                btnSubir.addActionListener(e -> {
                    int idx = listaSecuencia.getSelectedIndex();
                    if (idx > 0) {
                        String val = modeloSecuencia.remove(idx);
                        modeloSecuencia.add(idx - 1, val);
                        listaSecuencia.setSelectedIndex(idx - 1);
                    }
                });
                btnBajar.addActionListener(e -> {
                    int idx = listaSecuencia.getSelectedIndex();
                    if (idx >= 0 && idx < modeloSecuencia.size() - 1) {
                        String val = modeloSecuencia.remove(idx);
                        modeloSecuencia.add(idx + 1, val);
                        listaSecuencia.setSelectedIndex(idx + 1);
                    }
                });

                campos.add(txtIntervalo); nombres.add("intervaloMs");
                campos.add(txtDuracion); nombres.add("duracionMs");

                campos.add(new JTextField() {{ setName("orden"); }});
                nombres.add("orden");

                add(new JLabel()); // placeholder to keep focus order

                ok.addActionListener(event -> {
                    String orden = String.join(">", Collections.list(modeloSecuencia.elements()));
                    String intervaloVal = txtIntervalo.getText().trim();
                    String duracionVal = txtDuracion.getText().trim();
                    StringBuilder sb = new StringBuilder();
                    if (!orden.isEmpty()) sb.append("orden=").append(orden);
                    if (!intervaloVal.isEmpty()) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append("intervaloMs=").append(intervaloVal);
                    }
                    if (!duracionVal.isEmpty()) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append("duracionMs=").append(duracionVal);
                    }
                    paramSummary = sb.toString();
                    dispose();
                });
                cancel.addActionListener(event -> dispose());
                pack();
                setLocationRelativeTo(getOwner());
                return;
            }

            add(centro, BorderLayout.CENTER);
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

        private String extraerValor(String resumen, String clave) {
            if (resumen == null) return null;
            String[] partes = resumen.split(",");
            for (String p : partes) {
                String[] kv = p.trim().split("=");
                if (kv.length == 2 && kv[0].trim().equalsIgnoreCase(clave)) {
                    return kv[1].trim();
                }
            }
            return null;
        }

        private String defaultOrParam(String resumen, String clave, String porDefecto) {
            String val = extraerValor(resumen, clave);
            return val == null || val.isEmpty() ? porDefecto : val;
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
        private List<BlockNodeData> accionPrincipal = new ArrayList<>();
        private List<RandomActionEntry> randomPool = new ArrayList<>();
        private long inicioAccionMs;
        private long duracionActualMs = 4000;
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

        void runScript(List<BlockNodeData> data, List<RandomActionEntry> pool) {
            this.accionPrincipal = data == null ? new ArrayList<>() : new ArrayList<>(data);
            this.randomPool = pool == null ? new ArrayList<>() : pool;
            if (!accionPrincipal.isEmpty()) {
                this.actions = accionPrincipal;
                parseActions();
            } else {
                activarAleatoria();
            }
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
            duracionActualMs = leerDuracionAccion(actions);
            inicioAccionMs = System.currentTimeMillis();
        }

        private void activarAleatoria() {
            if (randomPool.isEmpty()) {
                actions = new ArrayList<>();
                return;
            }
            int totalPeso = randomPool.stream().mapToInt(r -> r.peso).sum();
            int pick = random.nextInt(Math.max(1, totalPeso));
            RandomActionEntry elegido = randomPool.get(0);
            int acumulado = 0;
            for (RandomActionEntry entry : randomPool) {
                acumulado += entry.peso;
                if (pick < acumulado) {
                    elegido = entry;
                    break;
                }
            }
            actions = clonarAccion(elegido.bloques);
            parseActions();
        }

        private List<BlockNodeData> clonarAccion(List<BlockNodeData> origen) {
            List<BlockNodeData> copia = new ArrayList<>();
            for (BlockNodeData d : origen) {
                copia.add(new BlockNodeData(d.baseName, d.paramSummary, d.x, d.y, d.width, d.height, d.fillColor));
            }
            return copia;
        }

        private long leerDuracionAccion(List<BlockNodeData> datos) {
            for (BlockNodeData n : datos) {
                if ("reproducir animación".equals(n.baseName)) {
                    return (long) parseParam(n.paramSummary, "duracionMs", 4000);
                }
            }
            return 4000;
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

            if (accionPrincipal.isEmpty() && !randomPool.isEmpty()) {
                long transcurrido = System.currentTimeMillis() - inicioAccionMs;
                if (transcurrido >= duracionActualMs) {
                    activarAleatoria();
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
