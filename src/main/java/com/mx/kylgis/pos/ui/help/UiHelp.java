package com.mx.kylgis.pos.ui.help;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.text.JTextComponent;

/**
 * Automatic localized Swing help for all KylGis panels.
 * Resources are embedded in the JAR next to this class; no user-dir locale
 * loader is ever consulted. Supports NetBeans forms and dynamic panels.
 */
public final class UiHelp {
    private static final Logger LOG=Logger.getLogger(UiHelp.class.getName());
    private static final String RESOURCE="/com/mx/kylgis/pos/ui/help/";
    private static final Properties CATALOG=load("ui_help_catalog.properties");
    // Do not memoize partially initialized Swing containers here: NetBeans
    // installs children during initComponents(), before all fields are assigned.
    private static final Map<Locale,Properties> LOCALES=Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile boolean installed;

    private UiHelp() {}

    public static synchronized void install() {
        if(installed || GraphicsEnvironment.isHeadless()) return;
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            try {
                if(event instanceof WindowEvent w && w.getID()==WindowEvent.WINDOW_OPENED)
                    refresh(w.getWindow());
                else if(event instanceof ContainerEvent e && e.getID()==ContainerEvent.COMPONENT_ADDED) {
                    nameFields(e.getContainer());
                    Component child=e.getChild();
                    SwingUtilities.invokeLater(() -> refresh(child));
                } else if(event instanceof MouseEvent e && e.getID()==MouseEvent.MOUSE_ENTERED)
                    update(e.getComponent());
                else if(event instanceof FocusEvent e && e.getID()==FocusEvent.FOCUS_GAINED)
                    update(e.getComponent());
            } catch(RuntimeException ex) { LOG.log(Level.FINE,"UI help update",ex); }
        }, AWTEvent.WINDOW_EVENT_MASK | AWTEvent.CONTAINER_EVENT_MASK |
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.FOCUS_EVENT_MASK);
        installed=true;
        for(Window window:Window.getWindows()) refresh(window);
    }

    /** Standalone GUI and test entry point. No dependence on production DB. */
    public static void refresh(Component component) {
        if(component==null) return;
        if(component instanceof Container c) {
            nameFields(c);
            update(c);
            for(Component child:c.getComponents()) refresh(child);
        } else update(component);
    }

    private static void nameFields(Container owner) {
        if(owner==null) return;
        for(Class<?> cls=owner.getClass();cls!=null &&
                cls.getName().startsWith("com.mx.kylgis.");cls=cls.getSuperclass()) {
            for(Field field:cls.getDeclaredFields()) {
                if(!Component.class.isAssignableFrom(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    if(field.get(owner) instanceof JComponent child
                            && child.getClientProperty("kylgis.help.id")==null) {
                        String id=cls.getName()+"#"+field.getName();
                        if(CATALOG.containsKey(id))
                            child.putClientProperty("kylgis.help.id",id);
                    }
                } catch(RuntimeException | IllegalAccessException ex) {
                    LOG.log(Level.FINEST,"UI help field inaccessible",ex);
                }
            }
        }
    }

    public static String text(String key) {
        return locale().getProperty("text."+key,key);
    }

    public static String tooltip(String key) {
        return locale().getProperty("tooltip."+key,
                locale().getProperty("text.common.help","Help for this control."));
    }

    public static void set(JComponent control,String key) {
        if(control!=null) {
            control.putClientProperty("kylgis.help.override",key);
            control.setToolTipText(tooltip(key));
        }
    }

    /** Tooltip with a localized template and stable per-control context. */
    public static void setContext(JComponent control,String key,String context) {
        if(control==null)return;
        control.putClientProperty("kylgis.help.override",key);
        control.putClientProperty("kylgis.help.context",context);
        control.setToolTipText(tooltip(key).replace("{0}",context));
    }

    public static List<String> audit(Component component) {
        refresh(component);
        List<String> missing=new ArrayList<>();
        audit(component,missing);
        return missing;
    }
    private static void audit(Component component,List<String> missing) {
        if(component instanceof JComponent c && relevant(c) &&
           (c.getToolTipText()==null || c.getToolTipText().isBlank()))
            missing.add(String.valueOf(c.getClientProperty("kylgis.help.id")));
        if(component instanceof Container container)
            for(Component child:container.getComponents()) audit(child,missing);
    }

    private static void update(Component target) {
        if(!(target instanceof JComponent c)) return;
        localizedCaptions(c);
        if(c instanceof JTabbedPane tabs) {
            for(int i=0;i<tabs.getTabCount();i++) {
                String caption=tabs.getTitleAt(i);
                String translation=literal(caption);
                if(!translation.equals(caption)) tabs.setTitleAt(i,translation);
                tabs.setToolTipTextAt(i,describe("tab",tabs.getTitleAt(i)));
            }
        }
        if(!relevant(c)) return;
        String result=resolve(c);
        c.setToolTipText(result);
        if(c instanceof JTable table && table.getTableHeader()!=null)
            table.getTableHeader().setToolTipText(result);
        if(c instanceof JComboBox<?> combo && combo.isEditable() &&
           combo.getEditor().getEditorComponent() instanceof JComponent editor)
            editor.setToolTipText(result);
    }

    private static boolean relevant(JComponent c) {
        return c instanceof AbstractButton ||
                (c instanceof JLabel && c.getMouseListeners().length>0) ||
                c instanceof JTextComponent ||
                c instanceof JComboBox<?> || c instanceof JTable || c instanceof JList<?> ||
                c instanceof JTree || c instanceof JTabbedPane || c instanceof JSpinner ||
                c instanceof JSlider || c instanceof JScrollBar;
    }
    private static String kind(JComponent c) {
        if(c instanceof JTabbedPane) return "tab";
        if(c instanceof JCheckBox) return "check";
        if(c instanceof JRadioButton) return "radio";
        if(c instanceof JComboBox) return "select";
        if(c instanceof JPasswordField) return "password";
        if(c instanceof JTextComponent) return "input";
        if(c instanceof JTable) return "table";
        if(c instanceof JList || c instanceof JTree) return "list";
        if(c instanceof JSpinner || c instanceof JSlider || c instanceof JScrollBar) return "range";
        return "button";
    }

    private static String resolve(JComponent c) {
        Object custom=c.getClientProperty("kylgis.help.override");
        if(custom instanceof String key) {
            String template=tooltip(key);
            Object detail=c.getClientProperty("kylgis.help.context");
            return detail==null?template:template.replace("{0}",String.valueOf(detail));
        }
        Object id=c.getClientProperty("kylgis.help.id");
        // Curated, class+field-specific help wins over misleading legacy
        // tooltips (for example the user selector once said "Get Customers").
        if(id instanceof String key) {
            String reviewed=locale().getProperty("specific."+key);
            if(reviewed!=null && !reviewed.isBlank()) return reviewed;
        }
        // Specific tooltip copy from the original POS resource bundles is
        // captured at build time. Never read mutable user-language files here.
        if(id instanceof String key) {
            String nativeTooltip=locale().getProperty("native."+key);
            if(nativeTooltip!=null && !nativeTooltip.isBlank()) return nativeTooltip;
        }
        // Otherwise a visible caption or associated JLabel may be more
        // informative than a generic source field identifier.
        String subject=subject(c);
        if(!subject.isBlank()) return describe(kind(c),subject);
        if(id instanceof String key) {
            String explicit=locale().getProperty("control."+key);
            if(explicit!=null) return explicit;
            String meta=CATALOG.getProperty(key,"");
            int split=meta.indexOf('|');
            if(split>=0) subject=displayTokens(meta.substring(split+1));
        }
        if(subject.isBlank()) subject=displayTokens(c.getName());
        if(subject.isBlank()) subject=locale().getProperty("target."+kind(c),"the selected value");
        return describe(kind(c),subject);
    }
    private static String describe(String kind,String subject) {
        String pattern=locale().getProperty("format."+kind,
                locale().getProperty("format.input","Edit {0}."));
        return MessageFormat.format(pattern,subject);
    }
    private static String subject(JComponent component) {
        if(component instanceof AbstractButton button && meaningful(button.getText()))
            return strip(button.getText());
        if(component.getAccessibleContext()!=null &&
           meaningful(component.getAccessibleContext().getAccessibleName()))
            return strip(component.getAccessibleContext().getAccessibleName());
        Container parent=component.getParent();
        if(parent!=null) {
            // The JLabel.for contract is authoritative even before layout.
            for(Component sibling:parent.getComponents()) {
                if(sibling instanceof JLabel label
                        && label.getLabelFor()==component
                        && meaningful(label.getText()))
                    return strip(label.getText());
            }
            // Never infer adjacency while components have zero bounds.
            // GridLayout/NetBeans forms first add controls at (0,0), so the
            // old nearest-label heuristic could assign an unrelated caption.
            if(component.getWidth()<=0 || component.getHeight()<=0)
                return "";
            JLabel nearest=null; double distance=Double.POSITIVE_INFINITY;
            for(Component sibling:parent.getComponents()) {
                if(!(sibling instanceof JLabel label)
                        ||!meaningful(label.getText()) || !label.isVisible()
                        ||label.getWidth()<=0 || label.getHeight()<=0)
                    continue;
                double labelCenterY=label.getY()+label.getHeight()/2.0;
                double controlCenterY=component.getY()+component.getHeight()/2.0;
                if(Math.abs(labelCenterY-controlCenterY)>
                        Math.max(8.0,Math.max(label.getHeight(),component.getHeight())*0.75))
                    continue;
                // Only labels immediately to the left of the editable field.
                double separation=component.getX()-(label.getX()+label.getWidth());
                if(separation < -8 || separation > 180) continue;
                if(separation<distance) {distance=separation;nearest=label;}
            }
            if(nearest!=null) return strip(nearest.getText());
        }
        return "";
    }
    private static boolean meaningful(String text) {
        String s=strip(text);
        return !s.isBlank() && !s.matches("[0-9.+\\-xX* ]*") &&
                !s.matches("(?i)j(?:label|button|textfield|combobox)\\d+");
    }
    private static String strip(String text) {
        return text==null?"":text.replaceAll("<[^>]*>"," ").replaceAll("\\s+"," ").trim();
    }
    private static String displayTokens(String raw) {
        if(raw==null)return "";
        StringBuilder s=new StringBuilder();
        for(String token:raw.split("[^A-Za-z0-9]+")) {
            if(token.isBlank()) continue;
            if(s.length()>0)s.append(' ');
            s.append(locale().getProperty("term."+token.toLowerCase(Locale.ROOT),token));
        }
        return s.toString();
    }
    private static void localizedCaptions(JComponent c) {
        if(c instanceof JLabel label && label.getText()!=null)
            label.setText(literal(label.getText()));
        if(c instanceof AbstractButton button && button.getText()!=null)
            button.setText(literal(button.getText()));
        localizeBorder(c.getBorder());
    }

    private static void localizeBorder(Border border) {
        if(border instanceof TitledBorder title && title.getTitle()!=null) {
            title.setTitle(literal(title.getTitle()));
            localizeBorder(title.getBorder());
        } else if(border instanceof CompoundBorder compound) {
            localizeBorder(compound.getOutsideBorder());
            localizeBorder(compound.getInsideBorder());
        }
    }

    private static String literal(String text) {
        if(text==null || text.isBlank())return text;
        if(text.startsWith("<html")) {
            // Localize the user-facing welcome title but leave the legal
            // GPL notice and HTML structure untouched.
            String legacyTitle="KylGis POS Punto de Venta Táctil";
            return text.contains(legacyTitle)
                    ? text.replace(legacyTitle,text("branding.title")) : text;
        }
        String translation=locale().getProperty("literal."+digest(text));
        return translation==null?text:translation;
    }
    private static String digest(String text) {
        try {
            byte[] bytes=MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder s=new StringBuilder();
            for(int i=0;i<8;i++) s.append(String.format("%02x",bytes[i]&0xff));
            return s.toString();
        } catch(NoSuchAlgorithmException ex) {throw new IllegalStateException(ex);}
    }
    private static Properties locale() {
        Locale l=Locale.getDefault();
        synchronized(LOCALES){return LOCALES.computeIfAbsent(l,UiHelp::buildLocale);}
    }
    private static Properties buildLocale(Locale l) {
        Properties p=load("ui_help.properties");
        if(!l.getLanguage().isBlank()) {
            p.putAll(load("ui_help_"+l.getLanguage()+".properties"));
            if(!l.getCountry().isBlank())
                p.putAll(load("ui_help_"+l.getLanguage()+"_"+l.getCountry()+".properties"));
        }
        return p;
    }
    private static Properties load(String filename) {
        Properties p=new Properties();
        try(InputStream in=jarResource(filename)) {
            if(in!=null)p.load(new InputStreamReader(in,StandardCharsets.UTF_8));
        }catch(IOException e){LOG.log(Level.WARNING,"Embedded UI help: "+filename,e);}
        return p;
    }

    /**
     * When deployed, read directly from the exact JAR that contains UiHelp.class.
     * Neither user.dir/locales, working-directory resources, nor earlier classpath
     * resources can shadow the descriptions. IDE source-directory runs still work.
     */
    private static InputStream jarResource(String filename) throws IOException {
        java.security.CodeSource source=UiHelp.class.getProtectionDomain().getCodeSource();
        if(source!=null && source.getLocation()!=null
                && source.getLocation().getPath().endsWith(".jar")) {
            try {
                java.io.File packageJar=new java.io.File(source.getLocation().toURI());
                try(java.util.jar.JarFile jar=new java.util.jar.JarFile(packageJar)) {
                    java.util.jar.JarEntry entry=jar.getJarEntry(RESOURCE.substring(1)+filename);
                    if(entry==null) return null;
                    try(InputStream in=jar.getInputStream(entry)) {
                        return new java.io.ByteArrayInputStream(in.readAllBytes());
                    }
                }
            } catch(java.net.URISyntaxException ex) {
                throw new IOException("Invalid bundled application JAR location",ex);
            }
        }
        return UiHelp.class.getResourceAsStream(RESOURCE+filename);
    }
}
