import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.TransferHandler;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.DataFlavor;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.FontMetrics;

public class MascotaEscritorioIDE extends JFrame {

    // Para las "acciones" del panel central + tabla de acciones aleatorias
    private JTabbedPane tabsAcciones;
    private DefaultTableModel modeloTablaAcciones;
    private JTable tablaAcciones;

    public MascotaEscritorioIDE() {
        setTitle("Editor de Mascotas de Escritorio - Tipo Scratch");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);

        setJMenuBar(crearMenu());
        setContentPane(crearContenidoPrincipal());
    }

    // ---------------- MENÚ SUPERIOR ----------------

    private JMenuBar crearMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu archivo = new JMenu("Archivo");
        archivo.add(new JMenuItem("Nuevo proyecto"));
        archivo.add(new JMenuItem("Abrir..."));
        archivo.add(new JMenuItem("Guardar"));
        archivo.addSeparator();
        archivo.add(new JMenuItem("Salir"));

        JMenu editar = new JMenu("Editar");
        editar.add(new JMenuItem("Deshacer"));
        editar.add(new JMenuItem("Rehacer"));

        JMenu ver = new JMenu("Ver");
        ver.add(new JMenuItem("Mostrar/ocultar panel animaciones"));
        ver.add(new JMenuItem("Tema oscuro (futuro)"));

        JMenu ayuda = new JMenu("Ayuda");
        ayuda.add(new JMenuItem("Tutorial rápido"));
        ayuda.add(new JMenuItem("Acerca de"));

        menuBar.add(archivo);
        menuBar.add(editar);
        menuBar.add(ver);
        menuBar.add(ayuda);

        return menuBar;
    }

    // ------------- CONTENEDOR PRINCIPAL -------------

    private JPanel crearContenidoPrincipal() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Barra de herramientas superior
        root.add(crearBarraHerramientas(), BorderLayout.NORTH);

        // Panel izquierdo: paleta de bloques
        JPanel panelPaleta = crearPanelPaletaBloques();

        // Panel central: scripts de la mascota (acciones)
        JPanel panelScripts = crearPanelScripts();

        // Panel derecho: vista previa scripts + acciones aleatorias
        JPanel panelDerecho = crearPanelDerecho();

        root.add(panelPaleta, BorderLayout.WEST);
        root.add(panelScripts, BorderLayout.CENTER);
        root.add(panelDerecho, BorderLayout.EAST);

        return root;
    }

    // ------------- BARRA DE HERRAMIENTAS -------------

    private JToolBar crearBarraHerramientas() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(new JLabel("Mascota: "));
        JComboBox<String> comboMascota = new JComboBox<>(new String[]{
                "Gatito", "Slime", "Dragón", "Fantasma"
        });
        toolBar.add(comboMascota);

        toolBar.addSeparator();

        JButton btnProbar = new JButton("▶ Probar");
        JButton btnDetener = new JButton("⏹ Detener");

        toolBar.add(btnProbar);
        toolBar.add(btnDetener);

        toolBar.addSeparator();

        JButton btnProbarEscritorio = new JButton("Probar en escritorio");
        toolBar.add(btnProbarEscritorio);

        return toolBar;
    }

    // ------------- PANEL IZQUIERDO: PALETA -----------

    private JPanel crearPanelPaletaBloques() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Paleta de bloques"));
        panel.setPreferredSize(new Dimension(260, 0));
        panel.setBackground(new Color(240, 248, 255));

        // ---- Categorías y bloques definidos por ti ----
        String[][] datosCategorias = {
                {
                        "Eventos",
                        "cuando inicia la mascota",
                        "cuando se hace clic en la mascota",
                        "cuando se arrastra la mascota",
                        "cuando se suelta la mascota",
                        "cuando pasan [ x ] segundos sin interacción",
                        "cuando colisiona con bordes de pantalla",
                        "cuando colisiona con otra mascota",
                        "cuando variable mayor o menor que",
                        "cambiar valor de variable"
                },
                {
                        "Movimiento",
                        "mover",
                        "seguir al mouse",
                        "escapar del mouse",
                        "seguir a mascota",
                        "teletransportar"
                },
                {
                        "Animación",
                        "reproducir animación",
                        "agrandar mascota",
                        "achicar mascota",
                        "voltear horizontalmente",
                        "voltear verticalmente"
                }
        };

        // Mapa categoría -> bloques
        Map<String, String[]> bloquesPorCategoria = new LinkedHashMap<>();

        DefaultListModel<String> modeloCategorias = new DefaultListModel<>();
        DefaultListModel<String> modeloBloques = new DefaultListModel<>();

        for (String[] cat : datosCategorias) {
            String nombreCat = cat[0];
            modeloCategorias.addElement(nombreCat);

            String[] bloques = new String[cat.length - 1];
            System.arraycopy(cat, 1, bloques, 0, bloques.length);
            bloquesPorCategoria.put(nombreCat, bloques);
        }

        // ---- Listas de categorías y bloques ----
        JList<String> listaCategorias = new JList<>(modeloCategorias);
        listaCategorias.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaCategorias.setBorder(
                BorderFactory.createTitledBorder("Categoría de bloque"));
        listaCategorias.setVisibleRowCount(6);

        JList<String> listaBloques = new JList<>(modeloBloques);
        listaBloques.setBorder(
                BorderFactory.createTitledBorder("Bloques de la categoría"));

        // NUEVO: permitir arrastrar bloques desde esta lista
        listaBloques.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaBloques.setDragEnabled(true);
        listaBloques.setTransferHandler(new BlockDragTransferHandler());


        // Actualizar bloques al cambiar la categoría seleccionada
        listaCategorias.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String cat = listaCategorias.getSelectedValue();
                modeloBloques.clear();
                if (cat != null) {
                    String[] bloques = bloquesPorCategoria.get(cat);
                    if (bloques != null) {
                        for (String b : bloques) {
                            modeloBloques.addElement(b);
                        }
                    }
                }
            }
        });

        // Seleccionar la primera categoría al inicio
        if (!modeloCategorias.isEmpty()) {
            listaCategorias.setSelectedIndex(0);
        }

        JScrollPane scrollCategorias = new JScrollPane(listaCategorias);
        JScrollPane scrollBloques = new JScrollPane(listaBloques);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                scrollCategorias, scrollBloques);
        split.setResizeWeight(0.35);

        // ---- Zona inferior: variables ----
        DefaultListModel<String> modeloVariables = new DefaultListModel<>();
        JList<String> listaVariables = new JList<>(modeloVariables);
        listaVariables.setBorder(BorderFactory.createTitledBorder("Variables"));

        JScrollPane scrollVariables = new JScrollPane(listaVariables);
        // altura mínima para que se vea bien
        scrollVariables.setPreferredSize(new Dimension(0, 120));

        JButton btnCrearVariable = new JButton("Crear variable");

        btnCrearVariable.addActionListener(e -> {
            String nombre = JOptionPane.showInputDialog(
                    panel,
                    "Nombre de la nueva variable:",
                    "Crear variable",
                    JOptionPane.PLAIN_MESSAGE
            );

            if (nombre == null) {
                // cancelado
                return;
            }

            nombre = nombre.trim();
            if (nombre.isEmpty()) {
                JOptionPane.showMessageDialog(
                        panel,
                        "El nombre de la variable no puede estar vacío.",
                        "Error",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            // evitar duplicados
            for (int i = 0; i < modeloVariables.size(); i++) {
                if (modeloVariables.getElementAt(i).equals(nombre)) {
                    JOptionPane.showMessageDialog(
                            panel,
                            "Ya existe una variable con ese nombre.",
                            "Error",
                            JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }
            }

            modeloVariables.addElement(nombre);
        });

        JPanel panelVariables = new JPanel(new BorderLayout());
        panelVariables.add(btnCrearVariable, BorderLayout.NORTH);
        panelVariables.add(scrollVariables, BorderLayout.CENTER);

        // Contenedor central: arriba categorías/bloques, abajo variables
        JPanel contenedorCentro = new JPanel(new BorderLayout());
        contenedorCentro.add(split, BorderLayout.CENTER);
        contenedorCentro.add(panelVariables, BorderLayout.SOUTH);

        panel.add(contenedorCentro, BorderLayout.CENTER);
        return panel;
    }


    // ------------ PANEL CENTRAL: SCRIPTS (ACCIONES) ------------

    private JPanel crearPanelScripts() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Scripts de la mascota"));

        // Pestañas SOLO para acciones creadas
        tabsAcciones = new JTabbedPane();

        // ----- Barra superior con botones "Crear acción" y "Eliminar acción" -----
        JPanel barraAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton btnCrearAccion = new JButton("Crear acción");
        JButton btnEliminarAccion = new JButton("Eliminar acción");

        barraAcciones.add(btnCrearAccion);
        barraAcciones.add(btnEliminarAccion);

        // ----- Lógica botón "Crear acción" -----
        btnCrearAccion.addActionListener(e -> {
            String nombre = JOptionPane.showInputDialog(
                    panel,
                    "Nombre de la nueva acción:",
                    "Nueva acción",
                    JOptionPane.PLAIN_MESSAGE
            );

            if (nombre == null) {
                // Cancelado
                return;
            }

            nombre = nombre.trim();
            if (nombre.isEmpty()) {
                JOptionPane.showMessageDialog(
                        panel,
                        "El nombre no puede estar vacío.",
                        "Error",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            // En vez de un panel vacío, usamos nuestro lienzo de bloques
            BlockCanvasPanel nuevoScript = new BlockCanvasPanel();
            tabsAcciones.addTab(nombre, nuevoScript);
            tabsAcciones.setSelectedComponent(nuevoScript);

            // Si quieres ligarlo a la tabla de "Acciones aleatorias", dejamos esto:
            if (modeloTablaAcciones != null) {
                modeloTablaAcciones.addRow(new Object[]{
                        Boolean.TRUE,      // usar
                        nombre,            // nombre acción
                        Integer.valueOf(1) // peso por defecto
                });
            }
        });

        // ----- Lógica botón "Eliminar acción" -----
        btnEliminarAccion.addActionListener(e -> {
            int total = tabsAcciones.getTabCount();

            if (total == 0) {
                JOptionPane.showMessageDialog(
                        panel,
                        "No hay acciones para eliminar.",
                        "Eliminar acción",
                        JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }

            // Recolectar nombres de todas las acciones (todas las pestañas)
            List<String> nombresAcciones = new ArrayList<>();
            for (int i = 0; i < total; i++) {
                nombresAcciones.add(tabsAcciones.getTitleAt(i));
            }

            String seleccion = (String) JOptionPane.showInputDialog(
                    panel,
                    "Selecciona la acción a eliminar:",
                    "Eliminar acción",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    nombresAcciones.toArray(new String[0]),
                    nombresAcciones.get(0)
            );

            if (seleccion == null) {
                // Cancelado
                return;
            }

            // Eliminar pestaña
            for (int i = 0; i < tabsAcciones.getTabCount(); i++) {
                if (tabsAcciones.getTitleAt(i).equals(seleccion)) {
                    tabsAcciones.removeTabAt(i);
                    break;
                }
            }

            // Eliminar fila correspondiente en la tabla
            if (modeloTablaAcciones != null) {
                for (int r = 0; r < modeloTablaAcciones.getRowCount(); r++) {
                    Object value = modeloTablaAcciones.getValueAt(r, 1); // col 1 = nombre
                    if (seleccion.equals(value)) {
                        modeloTablaAcciones.removeRow(r);
                        break;
                    }
                }
            }
        });

        panel.add(barraAcciones, BorderLayout.NORTH);
        panel.add(tabsAcciones, BorderLayout.CENTER);

        return panel;
    }


    private JPanel crearPanelScriptVacio(String textoAyuda) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel label = new JLabel(
                "<html><body style='width:250px;'>" +
                        textoAyuda +
                        "<br><br>(Más adelante aquí iría tu sistema de bloques visuales)." +
                        "</body></html>"
        );
        label.setVerticalAlignment(SwingConstants.TOP);

        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    // ------------ PANEL DERECHO: PREVIEW + ACCIONES ALEATORIAS ------------

    private JPanel crearPanelDerecho() {
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setPreferredSize(new Dimension(360, 0));

        JPanel panelPreview = crearPanelPreview();
        JPanel panelAnimaciones = crearPanelAnimaciones();

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                panelPreview, panelAnimaciones);
        split.setResizeWeight(0.55);

        contenedor.add(split, BorderLayout.CENTER);
        return contenedor;
    }

    // ---- NUEVO PANEL DE PREVIEW: SCRIPTS + LISTA + DIBUJO 128x128 ----

    private JPanel crearPanelPreview() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Vista previa"));

        // Lista de scripts (modelo + lista)
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> listaScripts = new JList<>(listModel);
        JScrollPane scrollLista = new JScrollPane(listaScripts);
        scrollLista.setBorder(BorderFactory.createTitledBorder("Scripts creados"));

        // Canvas donde se ve el script seleccionado
        PreviewMascotaPanel canvas = new PreviewMascotaPanel();
        canvas.setPreferredSize(new Dimension(300, 260));

        // Lista interna con la info de cada script (nombre + pixeles)
        List<ScriptSprite> scripts = new ArrayList<>();

        // Barra superior con botones
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnCrearScript = new JButton("Crear script");
        JButton btnEliminarScript = new JButton("Eliminar script");
        barra.add(btnCrearScript);
        barra.add(btnEliminarScript);

        panel.add(barra, BorderLayout.NORTH);
        panel.add(canvas, BorderLayout.CENTER);
        panel.add(scrollLista, BorderLayout.SOUTH);

        // Crear script -> abre diálogo con cuadrícula 128x128
        btnCrearScript.addActionListener(e -> {
            ScriptEditorDialog dialog =
                    new ScriptEditorDialog(MascotaEscritorioIDE.this);
            dialog.setVisible(true);
            ScriptSprite nuevo = dialog.getResult();
            if (nuevo != null) {
                scripts.add(nuevo);
                listModel.addElement(nuevo.getNombre());
                listaScripts.setSelectedIndex(listModel.size() - 1);
                canvas.setPixelData(nuevo.getPixels());
                canvas.repaint();
            }
        });

        // Eliminar script seleccionado
        btnEliminarScript.addActionListener(e -> {
            int index = listaScripts.getSelectedIndex();
            if (index == -1) {
                JOptionPane.showMessageDialog(
                        panel,
                        "Selecciona un script de la lista.",
                        "Eliminar script",
                        JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }

            String nombre = listModel.getElementAt(index);
            int confirm = JOptionPane.showConfirmDialog(
                    panel,
                    "¿Eliminar el script '" + nombre + "'?",
                    "Confirmar eliminación",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                listModel.remove(index);
                scripts.remove(index);

                if (!scripts.isEmpty()) {
                    int nuevoIndex = Math.min(index, scripts.size() - 1);
                    listaScripts.setSelectedIndex(nuevoIndex);
                    canvas.setPixelData(scripts.get(nuevoIndex).getPixels());
                } else {
                    canvas.setPixelData(null);
                }
                canvas.repaint();
            }
        });

        // Al cambiar de script en la lista, actualizar el canvas
        listaScripts.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int idx = listaScripts.getSelectedIndex();
                if (idx >= 0 && idx < scripts.size()) {
                    canvas.setPixelData(scripts.get(idx).getPixels());
                } else {
                    canvas.setPixelData(null);
                }
                canvas.repaint();
            }
        });

        return panel;
    }

    // ------------ PANEL DERECHO INFERIOR: ACCIONES ALEATORIAS ------------

    private JPanel crearPanelAnimaciones() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Acciones aleatorias"));

        String[] columnas = {"Usar", "Nombre acción", "Peso"};
        modeloTablaAcciones = new DefaultTableModel(columnas, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class;
                if (columnIndex == 2) return Integer.class;
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                // editable: Usar y Peso
                return column == 0 || column == 2;
            }
        };

        tablaAcciones = new JTable(modeloTablaAcciones);
        JScrollPane scroll = new JScrollPane(tablaAcciones);

        JPanel ayuda = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 4, 4);

        JLabel texto = new JLabel("<html>" +
                "Aquí se mostrarán las acciones creadas.<br>" +
                "Marca cuáles entran en el pool aleatorio y ajusta el peso." +
                "</html>");
        texto.setFont(texto.getFont().deriveFont(Font.PLAIN, 11f));

        ayuda.add(texto, gbc);

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(ayuda, BorderLayout.SOUTH);
        return panel;
    }

    // ------------ PANEL DE DIBUJO / PREVIEW PARA SCRIPT ------------

    private static class PreviewMascotaPanel extends JPanel {

        private Color[][] pixels; // null = sin script (se dibuja la carita por defecto)

        public PreviewMascotaPanel() {
            setBackground(new Color(250, 250, 250));
            setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        }

        public void setPixelData(Color[][] pixels) {
            this.pixels = pixels;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (pixels != null && pixels.length > 0 && pixels[0].length > 0) {
                int cols = pixels.length;
                int rows = pixels[0].length;

                int cellW = Math.max(1, getWidth() / cols);
                int cellH = Math.max(1, getHeight() / rows);

                for (int y = 0; y < rows; y++) {
                    for (int x = 0; x < cols; x++) {
                        int px = x * cellW;
                        int py = y * cellH;

                        Color c = pixels[x][y];
                        if (c != null) {
                            g.setColor(c);
                            g.fillRect(px, py, cellW, cellH);
                        } else {
                            // cuadricado gris para "transparente"
                            if (((x + y) & 1) == 0) {
                                g.setColor(new Color(230, 230, 230));
                            } else {
                                g.setColor(new Color(245, 245, 245));
                            }
                            g.fillRect(px, py, cellW, cellH);
                        }
                    }
                }

                g.setColor(new Color(200, 200, 200));
                for (int x = 0; x <= cols; x++) {
                    int px = x * cellW;
                    g.drawLine(px, 0, px, rows * cellH);
                }
                for (int y = 0; y <= rows; y++) {
                    int py = y * cellH;
                    g.drawLine(0, py, cols * cellW, py);
                }

            } else {
                // Carita de ejemplo si no hay script seleccionado
                int w = getWidth();
                int h = getHeight();
                int size = Math.min(w, h) / 2;
                int x = (w - size) / 2;
                int y = (h - size) / 2;

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
            }
        }
    }

    // ------------ CLASE PARA GUARDAR UN SCRIPT (nombre + pixeles) ------------

    private static class ScriptSprite {
        private final String nombre;
        private final Color[][] pixels;

        public ScriptSprite(String nombre, Color[][] pixels) {
            this.nombre = nombre;
            this.pixels = pixels;
        }

        public String getNombre() {
            return nombre;
        }

        public Color[][] getPixels() {
            return pixels;
        }
    }

    // ------------ DIÁLOGO DE EDICIÓN DE SCRIPT + CUADRÍCULA 128x128 ------------

    private static class ScriptEditorDialog extends JDialog {

        private JTextField txtNombre;
        private PixelGridPanel gridPanel;
        private Color currentColor = Color.BLACK;
        private ScriptSprite result;

        public ScriptEditorDialog(JFrame owner) {
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

            // Selección de color
            btnElegirColor.addActionListener(e -> {
                Color elegido = JColorChooser.showDialog(
                        this,
                        "Selecciona un color",
                        currentColor
                );
                if (elegido != null) {
                    currentColor = elegido;
                    muestraColor.setBackground(currentColor);
                    gridPanel.setCurrentColor(currentColor);
                }
            });

            // Borrador = transparente
            btnTransparente.addActionListener(e -> {
                currentColor = null;
                muestraColor.setBackground(new Color(220, 220, 220));
                gridPanel.setCurrentColor(null);
            });

            // Guardar
            btnGuardar.addActionListener(e -> {
                String nombre = txtNombre.getText().trim();
                if (nombre.isEmpty()) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Debes ingresar un nombre para el script.",
                            "Error",
                            JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }
                Color[][] datos = gridPanel.getPixelsCopy();
                result = new ScriptSprite(nombre, datos);
                dispose();
            });

            // Cancelar
            btnCancelar.addActionListener(e -> {
                result = null;
                dispose();
            });

            pack();
            setLocationRelativeTo(getOwner());
        }

        public ScriptSprite getResult() {
            return result;
        }
    }

    // ------------ PANEL DE CUADRÍCULA 128x128 PARA PINTAR ------------

    private static class PixelGridPanel extends JPanel {

        private final int cols;
        private final int rows;
        private final int cellSize;
        private final Color[][] pixels;
        private Color currentColor;

        public PixelGridPanel(int cols, int rows, int cellSize) {
            this.cols = cols;
            this.rows = rows;
            this.cellSize = cellSize;
            this.pixels = new Color[cols][rows];

            setPreferredSize(new Dimension(cols * cellSize, rows * cellSize));
            setBackground(Color.WHITE);

            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    paintAt(e.getX(), e.getY());
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    paintAt(e.getX(), e.getY());
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        public void setCurrentColor(Color currentColor) {
            this.currentColor = currentColor;
        }

        private void paintAt(int xPixel, int yPixel) {
            int x = xPixel / cellSize;
            int y = yPixel / cellSize;
            if (x < 0 || x >= cols || y < 0 || y >= rows) return;

            pixels[x][y] = currentColor; // null => transparente
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    int px = x * cellSize;
                    int py = y * cellSize;

                    Color c = pixels[x][y];
                    if (c != null) {
                        g.setColor(c);
                        g.fillRect(px, py, cellSize, cellSize);
                    } else {
                        // cuadricado gris para transparencia
                        if (((x + y) & 1) == 0) {
                            g.setColor(new Color(230, 230, 230));
                        } else {
                            g.setColor(new Color(245, 245, 245));
                        }
                        g.fillRect(px, py, cellSize, cellSize);
                    }
                }
            }

            g.setColor(new Color(200, 200, 200));
            for (int x = 0; x <= cols; x++) {
                int px = x * cellSize;
                g.drawLine(px, 0, px, rows * cellSize);
            }
            for (int y = 0; y <= rows; y++) {
                int py = y * cellSize;
                g.drawLine(0, py, cols * cellSize, py);
            }
        }

        public Color[][] getPixelsCopy() {
            Color[][] copy = new Color[cols][rows];
            for (int x = 0; x < cols; x++) {
                for (int y = 0; y < rows; y++) {
                    copy[x][y] = pixels[x][y];
                }
            }
            return copy;
        }
    }
 // -----------------------------------------
 // Transferencia de bloques DESDE la paleta
 // -----------------------------------------
 private static class BlockDragTransferHandler extends TransferHandler {

     @Override
     protected Transferable createTransferable(javax.swing.JComponent c) {
         if (c instanceof JList) {
             @SuppressWarnings("rawtypes")
             JList list = (JList) c;
             Object value = list.getSelectedValue();
             if (value != null) {
                 return new StringSelection(value.toString());
             }
         }
         return null;
     }

     @Override
     public int getSourceActions(javax.swing.JComponent c) {
         return COPY;
     }
 }
//--------------------------
//Nodo que representa bloque
//--------------------------
private static class BlockNode {

  String baseName;       // nombre del bloque, ej: "mover"
  String paramSummary;   // texto con parámetros, ej: "dir=90°, vel=5"
  int x, y, width, height;
  Color fillColor;

  BlockNode(String baseName, int x, int y, int width, int height, Color fillColor) {
      this.baseName = baseName;
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.fillColor = fillColor;
      this.paramSummary = "";
  }

  String getFullText() {
      if (paramSummary == null || paramSummary.isEmpty()) {
          return baseName;
      }
      return baseName + " [" + paramSummary + "]";
  }

  boolean contains(int px, int py) {
      return px >= x && px <= x + width && py >= y && py <= y + height;
  }
}
//-------------------------------------------
//Panel donde se muestran y editan los bloques
//-------------------------------------------
private static class BlockCanvasPanel extends JPanel {

 private final java.util.List<BlockNode> blocks = new ArrayList<>();
 private BlockNode draggingBlock = null;
 private int dragOffsetX;
 private int dragOffsetY;

 public BlockCanvasPanel() {
     setBackground(new Color(250, 250, 250));
     setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
     setPreferredSize(new Dimension(400, 400));

     // Aceptar drops de bloques
     setTransferHandler(new BlockDropTransferHandler(this));

     // Mouse: arrastrar bloques y doble clic para editar
     MouseAdapter mouse = new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent e) {
             BlockNode n = findBlockAt(e.getX(), e.getY());
             if (n != null) {
                 draggingBlock = n;
                 dragOffsetX = e.getX() - n.x;
                 dragOffsetY = e.getY() - n.y;
             } else {
                 draggingBlock = null;
             }
         }

         @Override
         public void mouseReleased(MouseEvent e) {
             draggingBlock = null;
             repaint();
         }

         @Override
         public void mouseDragged(MouseEvent e) {
             if (draggingBlock != null) {
                 draggingBlock.x = e.getX() - dragOffsetX;
                 draggingBlock.y = e.getY() - dragOffsetY;
                 repaint();
             }
         }

         @Override
         public void mouseClicked(MouseEvent e) {
             if (e.getClickCount() == 2) {
                 BlockNode n = findBlockAt(e.getX(), e.getY());
                 if (n != null) {
                     // Editar bloque
                     Window w = SwingUtilities.getWindowAncestor(BlockCanvasPanel.this);
                     BlockEditDialog dialog = new BlockEditDialog(w, n);
                     dialog.setVisible(true);
                     repaint();
                 }
             }
         }
     };

     addMouseListener(mouse);
     addMouseMotionListener(mouse);
 }

 // Añadir bloque al lienzo (desde el drop)
 public void addBlockFromPalette(String blockName) {
     int baseX = 30;
     int baseY = 20 + blocks.size() * 60;
     int w = 240;
     int h = 40;
     Color c = pickColorFor(blockName);
     BlockNode node = new BlockNode(blockName, baseX, baseY, w, h, c);
     blocks.add(node);
     repaint();
 }

 private Color pickColorFor(String name) {
     // Colores según categoría aproximada
     if (name.startsWith("cuando")) {
         return new Color(255, 220, 160); // Eventos
     } else if (
             name.equals("mover") ||
             name.equals("seguir al mouse") ||
             name.equals("escapar del mouse") ||
             name.equals("seguir a mascota") ||
             name.equals("teletransportar")
     ) {
         return new Color(180, 220, 255); // Movimiento
     } else {
         return new Color(210, 190, 255); // Animación u otros
     }
 }

 private BlockNode findBlockAt(int x, int y) {
     // Buscar de arriba hacia abajo (último dibujado primero)
     for (int i = blocks.size() - 1; i >= 0; i--) {
         BlockNode n = blocks.get(i);
         if (n.contains(x, y)) {
             return n;
         }
     }
     return null;
 }

 @Override
 protected void paintComponent(Graphics g) {
     super.paintComponent(g);

     // Líneas de conexión: según orden vertical
     java.util.List<BlockNode> sorted = new ArrayList<>(blocks);
     sorted.sort((a, b) -> Integer.compare(a.y, b.y));

     g.setColor(new Color(150, 150, 150));
     for (int i = 0; i < sorted.size() - 1; i++) {
         BlockNode a = sorted.get(i);
         BlockNode b = sorted.get(i + 1);

         int x1 = a.x + a.width / 2;
         int y1 = a.y + a.height;
         int x2 = b.x + b.width / 2;
         int y2 = b.y;
         g.drawLine(x1, y1, x2, y2);
     }

     // Bloques
     for (BlockNode n : blocks) {
         g.setColor(n.fillColor != null ? n.fillColor : Color.LIGHT_GRAY);
         g.fillRoundRect(n.x, n.y, n.width, n.height, 12, 12);

         g.setColor(Color.DARK_GRAY);
         g.drawRoundRect(n.x, n.y, n.width, n.height, 12, 12);

         g.setColor(Color.BLACK);
         String text = n.getFullText();
         FontMetrics fm = g.getFontMetrics();
         int tx = n.x + 8;
         int ty = n.y + (n.height + fm.getAscent() - fm.getDescent()) / 2;
         // recortar si es muy largo
         if (text.length() > 35) {
             text = text.substring(0, 32) + "...";
         }
         g.drawString(text, tx, ty);
     }
 }

 // TransferHandler para recibir bloques como String
 private static class BlockDropTransferHandler extends TransferHandler {
     private final BlockCanvasPanel canvas;

     BlockDropTransferHandler(BlockCanvasPanel canvas) {
         this.canvas = canvas;
     }

     @Override
     public boolean canImport(TransferHandler.TransferSupport support) {
         return support.isDataFlavorSupported(DataFlavor.stringFlavor);
     }

     @Override
     public boolean importData(TransferHandler.TransferSupport support) {
         if (!canImport(support)) return false;
         try {
             Transferable t = support.getTransferable();
             String blockName = (String) t.getTransferData(DataFlavor.stringFlavor);
             canvas.addBlockFromPalette(blockName);
             return true;
         } catch (Exception ex) {
             ex.printStackTrace();
             return false;
         }
         
     }

 }
}
//-------------------------------------------------
//Diálogo para editar la lógica / parámetros del bloque
//-------------------------------------------------
private static class BlockEditDialog extends JDialog {

 private final BlockNode node;

 public BlockEditDialog(Window owner, BlockNode node) {
	    super(owner, "Editar bloque: " + node.baseName);
	    setModal(true);     // <- hace que sea modal
	    this.node = node;
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

     JLabel lblTitulo = new JLabel("Bloque: " + node.baseName);
     lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 14f));
     add(lblTitulo, BorderLayout.NORTH);

     java.util.List<JTextField> campos = new ArrayList<>();
     java.util.List<String> nombresCampos = new ArrayList<>();

     String name = node.baseName;

     if ("mover".equals(name)) {
         // Dirección / velocidad (simple)
         JLabel l1 = new JLabel("Dirección (grados 0-360):");
         JTextField t1 = new JTextField(8);
         JLabel l2 = new JLabel("Velocidad (px):");
         JTextField t2 = new JTextField(8);

         centro.add(l1, gbc);
         gbc.gridx = 1;
         centro.add(t1, gbc);
         gbc.gridx = 0;
         gbc.gridy++;

         centro.add(l2, gbc);
         gbc.gridx = 1;
         centro.add(t2, gbc);

         campos.add(t1);
         nombresCampos.add("dir");
         campos.add(t2);
         nombresCampos.add("vel");

     } else if ("seguir al mouse".equals(name) || "escapar del mouse".equals(name)) {
         JLabel l1 = new JLabel("Velocidad (px):");
         JTextField t1 = new JTextField(8);

         centro.add(l1, gbc);
         gbc.gridx = 1;
         centro.add(t1, gbc);

         campos.add(t1);
         nombresCampos.add("vel");

     } else if ("seguir a mascota".equals(name)) {
         JLabel l1 = new JLabel("Nombre de la otra mascota:");
         JTextField t1 = new JTextField(10);
         gbc.gridwidth = 1;
         centro.add(l1, gbc);
         gbc.gridx = 1;
         centro.add(t1, gbc);
         gbc.gridx = 0;
         gbc.gridy++;

         JLabel l2 = new JLabel("Velocidad (px):");
         JTextField t2 = new JTextField(8);
         centro.add(l2, gbc);
         gbc.gridx = 1;
         centro.add(t2, gbc);

         campos.add(t1);
         nombresCampos.add("mascota");
         campos.add(t2);
         nombresCampos.add("vel");

     } else if ("teletransportar".equals(name)) {
         JLabel l1 = new JLabel("Posición X:");
         JTextField t1 = new JTextField(6);
         JLabel l2 = new JLabel("Posición Y:");
         JTextField t2 = new JTextField(6);

         centro.add(l1, gbc);
         gbc.gridx = 1;
         centro.add(t1, gbc);
         gbc.gridx = 0;
         gbc.gridy++;

         centro.add(l2, gbc);
         gbc.gridx = 1;
         centro.add(t2, gbc);

         campos.add(t1);
         nombresCampos.add("x");
         campos.add(t2);
         nombresCampos.add("y");

     } else if (name.startsWith("cuando pasan")) {
         JLabel l1 = new JLabel("Segundos sin interacción:");
         JTextField t1 = new JTextField(8);

         centro.add(l1, gbc);
         gbc.gridx = 1;
         centro.add(t1, gbc);

         campos.add(t1);
         nombresCampos.add("segundos");

     } else if ("cuando variable mayor o menor que".equals(name)) {
         JLabel l1 = new JLabel("Nombre variable:");
         JTextField t1 = new JTextField(10);
         gbc.gridwidth = 1;
         centro.add(l1, gbc);
         gbc.gridx = 1;
         centro.add(t1, gbc);
         gbc.gridx = 0;
         gbc.gridy++;

         JLabel l2 = new JLabel("Operador (<, >, <=, >=):");
         JTextField t2 = new JTextField(4);
         centro.add(l2, gbc);
         gbc.gridx = 1;
         centro.add(t2, gbc);
         gbc.gridx = 0;
         gbc.gridy++;

         JLabel l3 = new JLabel("Valor umbral:");
         JTextField t3 = new JTextField(8);
         centro.add(l3, gbc);
         gbc.gridx = 1;
         centro.add(t3, gbc);

         campos.add(t1);
         nombresCampos.add("var");
         campos.add(t2);
         nombresCampos.add("op");
         campos.add(t3);
         nombresCampos.add("valor");

     } else if ("cambiar valor de variable".equals(name)) {
         JLabel l1 = new JLabel("Nombre variable:");
         JTextField t1 = new JTextField(10);
         gbc.gridwidth = 1;
         centro.add(l1, gbc);
         gbc.gridx = 1;
         centro.add(t1, gbc);
         gbc.gridx = 0;
         gbc.gridy++;

         JLabel l2 = new JLabel("Cambio (+/- número):");
         JTextField t2 = new JTextField(8);
         centro.add(l2, gbc);
         gbc.gridx = 1;
         centro.add(t2, gbc);

         campos.add(t1);
         nombresCampos.add("var");
         campos.add(t2);
         nombresCampos.add("delta");

     } else if ("reproducir animación".equals(name)) {
         JLabel l1 = new JLabel("Nombre de la animación/script:");
         JTextField t1 = new JTextField(12);

         centro.add(l1, gbc);
         gbc.gridx = 1;
         centro.add(t1, gbc);

         campos.add(t1);
         nombresCampos.add("anim");

     } else if ("agrandar mascota".equals(name) || "achicar mascota".equals(name)) {
         JLabel l1 = new JLabel("Porcentaje (+/-):");
         JTextField t1 = new JTextField(8);

         centro.add(l1, gbc);
         gbc.gridx = 1;
         centro.add(t1, gbc);

         campos.add(t1);
         nombresCampos.add("porc");

     } else {
         JLabel l = new JLabel("Este bloque no tiene parámetros editables.");
         centro.add(l, gbc);
     }

     add(centro, BorderLayout.CENTER);

     JPanel sur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
     JButton btnOk = new JButton("Guardar");
     JButton btnCancel = new JButton("Cancelar");
     sur.add(btnOk);
     sur.add(btnCancel);
     add(sur, BorderLayout.SOUTH);

     btnOk.addActionListener(e -> {
         if (!campos.isEmpty()) {
             StringBuilder sb = new StringBuilder();
             for (int i = 0; i < campos.size(); i++) {
                 String nombreCampo = nombresCampos.get(i);
                 String valor = campos.get(i).getText().trim();
                 if (!valor.isEmpty()) {
                     if (sb.length() > 0) sb.append(", ");
                     sb.append(nombreCampo).append("=").append(valor);
                 }
             }
             node.paramSummary = sb.toString();
         }
         dispose();
     });

     btnCancel.addActionListener(e -> {
         dispose();
     });

     pack();
     setLocationRelativeTo(getOwner());

 }
}

    // ----------------- MAIN -------------------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MascotaEscritorioIDE().setVisible(true);
        });
    }
}
