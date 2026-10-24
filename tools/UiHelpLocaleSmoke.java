import com.mx.kylgis.pos.config.JPanelConfigPlatforms;
import com.mx.kylgis.pos.config.JPanelConfigNodes;
import com.mx.kylgis.pos.config.JPanelConfigDatabase;
import com.mx.kylgis.pos.config.JPanelConfigSalesButtons;
import com.mx.kylgis.pos.scale.config.ScaleRequestSelectorPanel;
import com.mx.kylgis.pos.ui.help.UiHelp;
import com.mx.kylgis.pos.reports.JPanelReport;
import com.mx.kylgis.pos.reports.PanelReportBean;
import com.mx.kylgis.pos.reports.JParamsDatesInterval;
import java.awt.Component;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.swing.*;

/** Strict, no-DB, headless Swing coverage and localization smoke. */
public final class UiHelpLocaleSmoke {
    private static final String[] LOCALES={
        "en","al-SQ","ar","da","de","el","en-US","es","es-AR",
        "es-MX","et","fr","hr","it","nl","pt","pt-BR"
    };
    private static void require(boolean good,String error) {
        if(!good) throw new AssertionError(error);
    }
    private static void hasHelp(JComponent c,String id){
        String tip=c.getToolTipText();
        require(tip!=null && !tip.isBlank(),id+" missing tooltip");
        require(!tip.contains("**") && !tip.contains("tooltip."),id+" missing translation");
    }
    private static void collectChecks(Component root,List<JCheckBox> found) {
        if(root instanceof JCheckBox check)found.add(check);
        if(root instanceof java.awt.Container parent)
            for(Component child:parent.getComponents())collectChecks(child,found);
    }
    public static void main(String[] args) throws Exception {
        int locales=0,components=0;
        // A Swing component may be attached during NetBeans initComponents()
        // before its matching Java field is assigned. The second refresh must
        // still discover that field and assign its stable catalog identity.
        JPanelConfigPlatforms late=new JPanelConfigPlatforms();
        java.lang.reflect.Field delayedField=JPanelConfigPlatforms.class.getDeclaredField("newPlatform");
        delayedField.setAccessible(true);
        JTextField lateInput=(JTextField)delayedField.get(late);
        delayedField.set(late,null);
        UiHelp.refresh(late);
        delayedField.set(late,lateInput);
        UiHelp.refresh(late);
        require("com.mx.kylgis.pos.config.JPanelConfigPlatforms#newPlatform"
            .equals(lateInput.getClientProperty("kylgis.help.id")),
            "Delayed NetBeans field must be discovered when initialized later");
        System.out.println("KYLGIS_UI_HELP_LATE_NETBEANS_FIELD_PASS");
        for(String key:LOCALES) {
            Locale.setDefault(Locale.forLanguageTag(key));
            JPanel view=new JPanel();
            view.setLayout(new java.awt.GridLayout(0,1));
            JButton action=new JButton(UiHelp.text("platforms.add"));
            JTextField input=new JTextField();
            JComboBox<String> combo=new JComboBox<>(new String[]{"AUTO","ASCII:P"});
            combo.setEditable(true);
            JCheckBox check=new JCheckBox(UiHelp.text("salesbuttons.scale"));
            JRadioButton radio=new JRadioButton("AUTO");
            JLabel clickableImage=new JLabel("Buscar");
            clickableImage.addMouseListener(new java.awt.event.MouseAdapter(){});

            JTable table=new JTable(1,2);
            JList<String> list=new JList<>(new String[]{"A","B"});
            JSpinner spinner=new JSpinner();
            JTabbedPane tabs=new JTabbedPane();
            tabs.addTab("Menu",new JPanel());
            for(JComponent c:new JComponent[]{action,input,combo,check,radio,table,
                    list,spinner,tabs,clickableImage})view.add(c);
            UiHelp.refresh(view);
            for(Component c:view.getComponents()) {
                if(c instanceof JComponent jc) {hasHelp(jc,key+"/"+jc.getClass().getSimpleName());components++;}
            }
            // A label in an unlaid-out parent is never a valid implicit
            // label for every other control at the origin (0,0).
            require(!input.getToolTipText().contains("Buscar"),
                key+"/unrelated-unlaid-out-label-captured-input");
            require(!combo.getToolTipText().contains("Buscar"),
                key+"/unrelated-unlaid-out-label-captured-combo");
            // Explicit JLabel.labelFor must work even before the layout is
            // calculated, while unrelated labels on another row are ignored.
            JPanel named=new JPanel(null);
            JTextField namedField=new JTextField();
            JLabel relevantLabel=new JLabel("Customer code");
            relevantLabel.setLabelFor(namedField);
            named.add(relevantLabel);named.add(namedField);
            UiHelp.refresh(named);
            require(namedField.getToolTipText().contains("Customer code"),
                key+"/labelFor-is-authoritative");
            JPanel aligned=new JPanel(null);
            JTextField alignedField=new JTextField();
            JLabel alignedLabel=new JLabel("Valid field");
            JLabel distantLabel=new JLabel("Wrong row");
            alignedLabel.setBounds(10,10,80,24);
            alignedField.setBounds(96,10,135,24);
            distantLabel.setBounds(10,110,80,24);
            aligned.add(alignedLabel);aligned.add(alignedField);aligned.add(distantLabel);
            UiHelp.refresh(aligned);
            require(alignedField.getToolTipText().contains("Valid field")
                    && !alignedField.getToolTipText().contains("Wrong row"),
                key+"/laid-out-nearest-label-row");
            hasHelp((JComponent)combo.getEditor().getEditorComponent(),key+"/combo-editor");
            require(tabs.getToolTipTextAt(0)!=null,key+"/tab");
            require(table.getTableHeader().getToolTipText()!=null,key+"/table-header");
            JPanelConfigPlatforms platforms=new JPanelConfigPlatforms();
            UiHelp.refresh(platforms);
            require(UiHelp.audit(platforms).isEmpty(),key+"/platforms");
            JPanelConfigSalesButtons sales=new JPanelConfigSalesButtons();
            UiHelp.refresh(sales);
            require(UiHelp.audit(sales).isEmpty(),key+"/sales-buttons");
            ScaleRequestSelectorPanel scale=new ScaleRequestSelectorPanel();
            UiHelp.refresh(scale);
            require(UiHelp.audit(scale).isEmpty(),key+"/scale");
            JPanel decorated=new JPanel();
            decorated.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createEmptyBorder(1,1,1,1),
                javax.swing.BorderFactory.createTitledBorder("Campañas")));
            UiHelp.refresh(decorated);
            javax.swing.border.CompoundBorder compound =
                (javax.swing.border.CompoundBorder)decorated.getBorder();
            javax.swing.border.TitledBorder title =
                (javax.swing.border.TitledBorder)compound.getInsideBorder();
            String expectedTitle=key.startsWith("es") ? "Campañas" :
                key.equals("fr") ? "Campagnes" : key.equals("de") ? "Kampagnen" : null;
            if(expectedTitle!=null)
                require(expectedTitle.equals(title.getTitle()),key+"/nested-border-locale");
            JTextField productName=new JTextField();
            productName.putClientProperty("kylgis.help.id",
                "com.mx.kylgis.pos.inventory.ProductsEditor#m_jName");
            JTextField customerName=new JTextField();
            customerName.putClientProperty("kylgis.help.id",
                "com.mx.kylgis.pos.customers.CustomersView#m_jName");
            UiHelp.refresh(productName);
            UiHelp.refresh(customerName);
            require(!productName.getToolTipText().isBlank()
                    && !customerName.getToolTipText().isBlank()
                    && !productName.getToolTipText().equals(customerName.getToolTipText()),
                key+"/different-business-context-name-field");
            if(key.equals("es-MX")) {
                require(productName.getToolTipText().contains("Producto")
                    && !productName.getToolTipText().contains("Código de barras")
                    && customerName.getToolTipText().contains("Cliente"),
                    "Business help must not confuse product, barcode and customer");
            }
            for(String[] fieldRole:new String[][]{
                {"com.mx.kylgis.pos.reports.JParamsUser#btnUser","select.user"},
                {"com.mx.kylgis.pos.reports.JParamsCustomer#btnCustomer","select.customer"},
                {"com.mx.kylgis.pos.reports.JParamsSuppliers#btnSupplier","select.supplier"},
                {"com.mx.kylgis.pos.admin.JPeopleFinder#jButton1","filter.clear"},
                {"com.mx.kylgis.pos.admin.JPeopleFinder#jButton3","filter.apply"},
                {"com.mx.kylgis.pos.data.gui.JImageEditor#m_jbtnopen","image.open"},
                {"com.mx.kylgis.pos.data.gui.JImageEditor#m_jbtnclose","image.remove"},
                {"com.mx.kylgis.pos.data.gui.JImageEditor#m_jbtnzoomin","image.zoomIn"},
                {"com.mx.kylgis.pos.data.gui.JImageEditor#m_jbtnzoomout","image.zoomOut"},
                {"com.mx.kylgis.pos.sales.ReceiptSplit#jBtnToRightAll","split.all"},
                {"com.mx.kylgis.pos.sales.ReceiptSplit#jBtnToLeftAll","split.restoreAll"},
                {"com.mx.kylgis.pos.sales.restaurant.JTicketsBagRestaurant#m_DelTicket","order.delete"},
                {"com.mx.kylgis.pos.sales.restaurant.JTicketsBagRestaurant#j_btnKitchen","kitchen.send"},
                {"com.mx.kylgis.pos.sales.JPanelTicket#m_jbtnTicketPreview","ticket.preview"},
                {"com.mx.kylgis.pos.sales.JPanelTicket#m_jbtnPlatformOrder","platform.assign"}
            }) {
                JButton actualControl=new JButton();
                actualControl.putClientProperty("kylgis.help.id",fieldRole[0]);
                UiHelp.refresh(actualControl);
                String tip=actualControl.getToolTipText();
                require(tip!=null && !tip.isBlank() &&
                    !tip.equals("Help for this control.") &&
                    !tip.contains("jButton"),
                    key+"/curated-specific-control-"+fieldRole[0]);
                if(key.equals("es-MX") && fieldRole[0].contains("JParamsUser")) {
                    require(tip.contains("usuarios") && !tip.contains("clientes"),
                        "User finder must never describe customer selection");
                }
                if(key.equals("es-MX") && fieldRole[0].contains("m_jbtnTicketPreview")) {
                    require(tip.contains("sin finalizar la venta"),
                        "Receipt preview must not be confused with completing a sale");
                }
            }
            if(Locale.getDefault().getLanguage().equals("es")) {
                ResourceBundle legacy=ResourceBundle.getBundle("pos_messages",Locale.getDefault());
                require(legacy.getString("tooltip.config.db.user").contains("Nombre de usuario")
                        && !legacy.getString("tooltip.config.db.user").contains("contraseña de la cuenta"),
                    key+"/legacy-es-db-username-help");
                require("Aceptar".equals(legacy.getString("Button.OK")),
                    key+"/legacy-spanish-okay");
                require(legacy.getString("message.databasechange").contains("credenciales"),
                    key+"/legacy-spanish-db-error");
            }
            JPanelConfigDatabase dbPanel=new JPanelConfigDatabase();
            UiHelp.refresh(dbPanel);
            for(String fieldName:new String[]{"jtxtDbUser","jtxtDbUser1"}) {
                java.lang.reflect.Field field=JPanelConfigDatabase.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                JTextField userField=(JTextField)field.get(dbPanel);
                require(userField.getText()==null || userField.getText().isBlank(),
                    key+"/database-username-must-not-be-a-help-string");
                require(userField.getToolTipText().equals(UiHelp.tooltip("database.account.user")),
                    key+"/database-username-tooltip-"+fieldName);
            }
            for(String fieldName:new String[]{"jtxtDbPassword","jtxtDbPassword1"}) {
                java.lang.reflect.Field field=JPanelConfigDatabase.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                JComponent passwordField=(JComponent)field.get(dbPanel);
                require(passwordField.getToolTipText().equals(UiHelp.tooltip("database.account.password")),
                    key+"/database-password-tooltip-"+fieldName);
            }
            require(!UiHelp.tooltip("database.account.user").equals(UiHelp.tooltip("database.account.password")),
                key+"/database-username-and-password-distinct");
            java.lang.reflect.Field multiField=
                JPanelConfigDatabase.class.getDeclaredField("multiDB");
            multiField.setAccessible(true);
            JCheckBox multidb=(JCheckBox)multiField.get(dbPanel);
            require(multidb.getToolTipText().equals(UiHelp.tooltip("database.account.multidb")),
                key+"/database-multidb-toggle-help");
            java.lang.reflect.Field secondaryField=
                JPanelConfigDatabase.class.getDeclaredField("jtxtDbName1");
            secondaryField.setAccessible(true);
            JTextField secondaryName=(JTextField)secondaryField.get(dbPanel);
            boolean before=secondaryName.isEnabled();
            multidb.setSelected(!multidb.isSelected());
            for(java.awt.event.ActionListener action:multidb.getActionListeners()) {
                if(action.getClass().getName().contains("JPanelConfigDatabase")) {
                    action.actionPerformed(new java.awt.event.ActionEvent(
                        multidb,java.awt.event.ActionEvent.ACTION_PERFORMED,"toggle"));
                }
            }
            boolean after=secondaryName.isEnabled();
            require(before!=after,
                key+"/database-multidb-event-controls-secondary-fields");
            System.out.println("KYLGIS_DB_PANEL_RUNTIME_HELP_PASS locale="+key);
            PanelReportBean reportPanel=new PanelReportBean();
            UiHelp.refresh(reportPanel);
            require(UiHelp.audit(reportPanel).isEmpty(),key+"/report-panel-tooltip-coverage");
            for(String name:new String[]{"jToggleFilter","jButton1"}) {
                java.lang.reflect.Field field=JPanelReport.class.getDeclaredField(name);
                field.setAccessible(true);
                JComponent control=(JComponent)field.get(reportPanel);
                String expected=UiHelp.tooltip(name.equals("jButton1")
                    ? "report.execute" : "report.toggleFilters");
                require(expected.equals(control.getToolTipText()),
                    key+"/report-control-help-"+name);
            }
            JParamsDatesInterval interval=new JParamsDatesInterval();
            UiHelp.refresh(interval);
            for(String name:new String[]{"btnDateStart","btnDateEnd"}) {
                java.lang.reflect.Field field=JParamsDatesInterval.class.getDeclaredField(name);
                field.setAccessible(true);
                JComponent control=(JComponent)field.get(interval);
                String expected=UiHelp.tooltip(name.equals("btnDateStart")
                    ? "report.startCalendar" : "report.endCalendar");
                require(expected.equals(control.getToolTipText()),
                    key+"/report-period-calendar-"+name);
            }
            JPanelConfigNodes actualNodes=new JPanelConfigNodes();
            UiHelp.refresh(actualNodes);
            require(UiHelp.audit(actualNodes).isEmpty(),
                key+"/nodes-panel-tooltips");
            List<JCheckBox> actualRoles=new java.util.ArrayList<>();
            collectChecks(actualNodes,actualRoles);
            require(actualRoles.size()>=9,key+"/nodes-role-count");
            for(JCheckBox role:actualRoles) {
                require(role.getToolTipText()!=null &&
                    role.getToolTipText().contains(role.getText()) &&
                    !role.getToolTipText().contains("node.roles="),
                    key+"/nodes-role-help-"+role.getText());
            }
            java.lang.reflect.Field dbField=
                JPanelConfigNodes.class.getDeclaredField("dbName");
            dbField.setAccessible(true);
            JTextField dbInput=(JTextField)dbField.get(actualNodes);
            require(dbInput.getToolTipText()!=null &&
                    !dbInput.getToolTipText().isBlank(),
                    key+"/nodes-database-help");
            String[] configTabs={"general","locale","payment","peripherals",
                "system","ticket","company","database","sales_buttons","platforms","nodes"};
            for(String tab:configTabs) {
                String tabTitle=UiHelp.text("config.tab."+tab);
                String tip=UiHelp.tooltip("config.tab."+tab);
                require(tabTitle!=null && !tabTitle.isBlank() &&
                    !tabTitle.equals("config.tab."+tab),key+"/config-tab-title-"+tab);
                require(tip!=null && tip.contains(tabTitle),key+"/config-tab-help-"+tab);
            }
            if(key.equals("fr")) {
                require(UiHelp.text("config.tab.payment").equals("Modes de paiement"),
                    "French payments tab not translated");
            }
            if(key.equals("es-MX")) {
                require(UiHelp.text("config.tab.peripherals").equals("Periféricos"),
                    "Mexican Spanish peripheral tab not translated");
            }
            for(String k:new String[]{"peripheral.apply",
                    "peripheral.mirror.scroll"}) {
                String tip=UiHelp.tooltip(k);
                require(!tip.isBlank() && !tip.equals("Help for this control."),
                    key+"/specific-peripheral-help-"+k);
            }
            require(!UiHelp.text("config.error.save").isBlank(),
                key+"/localized-config-save-error");
            String bold=UiHelp.tooltip("product.editor.bold");
            String italic=UiHelp.tooltip("product.editor.italic");
            require(!bold.isBlank() && !italic.isBlank() && !bold.equals(italic),
                key+"/bold-and-italic-must-have-distinct-tooltips");
            if(key.equals("es-MX")) {
                require(bold.contains("negritas") && italic.contains("cursivas"),
                    "Product button font-style help is not localized correctly");
            }
            String mirror=UiHelp.text("peripheral.mirror.warning");
            String banner=UiHelp.text("peripheral.mirror.more");
            String ticketNote=UiHelp.text("peripheral.ticket.note");
            require(mirror.startsWith("<html>") && mirror.contains("Printer 1"), key+"/mirror-warning");
            require(banner.startsWith("<html>") && banner.endsWith("</html>"), key+"/mirror-banner");
            require(ticketNote.startsWith("<html>") && ticketNote.contains("Printer 1"),key+"/ticket-note");
            JLabel welcome=new JLabel("<html><center>KylGis POS Punto de Venta Táctil<br>Copyright KylGis POS</html>");
            UiHelp.refresh(welcome);
            require(welcome.getText().contains(UiHelp.text("branding.title")),key+"/branding-title");
            // If an old tooltip was available only in the base language,
            // it must NOT leak English into a different locale.
            JButton legacyOnly=new JButton();
            legacyOnly.putClientProperty("kylgis.help.id",
                "com.mx.kylgis.pos.customers.CustomersPayment#btnPay");
            UiHelp.refresh(legacyOnly);
            if(key.equals("fr"))
                require(!legacyOnly.getToolTipText().contains("Pay the account in full"),
                    "French fell back to unlocalized base tooltip");
            String scaleTip=UiHelp.tooltip("scale.request");
            require(scaleTip.contains("AUTO") && scaleTip.contains("ASCII"),key+"/protocol");
            String buttonText=UiHelp.text("salesbuttons.platform");
            require(!buttonText.isBlank() && !buttonText.equals("salesbuttons.platform"),key+"/caption");
            if(key.equals("es-MX")) {
                require(buttonText.contains("Código"),"Spanish locale ignored");
                require(scaleTip.contains("báscula"),"Spanish scale help ignored");
            }
            if(key.equals("fr")) require(buttonText.contains("commande"),"French locale ignored");
            locales++;
            System.out.println("EMBEDDED_UI_LOCALE_PASS locale="+key+
                " title="+buttonText+" inputTooltip="+input.getToolTipText());
        }
        System.out.println("KYLGIS_EMBEDDED_UI_HELP_QA_PASS locales="+locales+
            " testedControls="+components+" loadedFromJar=true");
    }
}
