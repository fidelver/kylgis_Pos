#!/usr/bin/env python3
"""Regenerate/validate embedded GUI help resources for all POS locales.

Only reads source code and existing translations. Never reads production DB.
Core templates have native translations. Any missing domain term is deliberately
kept as a technical identifier and reported by the audit, never invented.
"""
from __future__ import annotations
import argparse
import collections
import hashlib
import re
from pathlib import Path
from ui_help_extra_translations import PLATFORM_KEYS, PLATFORM_ERRORS, NODE_KEYS, NODE_TEXTS
from ui_help_static_translations import TRANSLATIONS as SYSTEM_UI_LABELS
from ui_help_static_campaigns import TRANSLATIONS as COUPON_UI_LABELS
from ui_help_static_system import TRANSLATIONS as OTHER_UI_LABELS
from ui_help_html_translations import TRANSLATIONS as LOCALIZED_HTML_TEXTS
from ui_help_config_tabs import TRANSLATIONS as LOCALIZED_CONFIG_TABS, TAB_KEYS
from ui_help_product_editor import TRANSLATIONS as LOCALIZED_PRODUCT_EDITOR
from ui_help_config_actions import MESSAGES as LOCALIZED_CONFIG_ACTIONS
from ui_help_vocabulary import TRANSLATIONS as LOCALIZED_TERMS
from ui_help_domain_hints import CONTEXTS as BUSINESS_FIELD_CONTEXTS
from ui_help_domain_translations import TRANSLATIONS as BUSINESS_DOMAIN_TRANSLATIONS
from ui_help_node_roles import TRANSLATIONS as LOCALIZED_NODE_ROLES
from ui_help_node_form import TRANSLATIONS as LOCALIZED_NODE_FORM
from ui_help_report_controls import TRANSLATIONS as LOCALIZED_REPORT_CONTROLS
from ui_help_operational_actions import COPY as OPERATIONAL_COPY, TARGETS as OPERATIONAL_TARGETS, LEGACY_CAPTIONS as OPERATIONAL_LEGACY_CAPTIONS
from ui_help_restaurant_actions import TRANSLATIONS as RESTAURANT_ACTION_COPY, TARGETS as RESTAURANT_ACTION_TARGETS
from ui_help_db_accounts import TRANSLATIONS as DATABASE_ACCOUNT_HELP

assert not (set(SYSTEM_UI_LABELS)&set(COUPON_UI_LABELS))
assert not (set(SYSTEM_UI_LABELS)&set(OTHER_UI_LABELS))
assert not (set(COUPON_UI_LABELS)&set(OTHER_UI_LABELS))
TRANSLATIONS={**SYSTEM_UI_LABELS,**COUPON_UI_LABELS,**OTHER_UI_LABELS}

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/com/mx/kylgis/pos"
RES = ROOT / "src/main/resources/com/mx/kylgis/pos/ui/help"
RES.mkdir(parents=True, exist_ok=True)

# Order: button, input, select, check, radio, table, list, tab,
# password, range, common-help, unknown-label.
PHRASES = {
    "base": (
        "Select «{0}» to perform this operation.",
        "Enter or edit «{0}» in the current form.",
        "Choose «{0}» from the available options.",
        "Enable or disable «{0}» in this panel.",
        "Select «{0}» as the active option.",
        "Review «{0}» and select a row for available operations.",
        "Review «{0}» and select an entry.",
        "Open the «{0}» tab to access its settings or information.",
        "Enter the password for «{0}»; its characters are hidden.",
        "Adjust «{0}» to set the desired value.",
        "Information and available operations for this control.",
        "this setting",
    ),
    "al_SQ": (
        "Zgjidhni «{0}» për të kryer këtë veprim.",
        "Shkruani ose ndryshoni «{0}» në formular.",
        "Zgjidhni «{0}» nga opsionet e disponueshme.",
        "Aktivizoni ose çaktivizoni «{0}» në këtë panel.",
        "Zgjidhni «{0}» si opsionin aktiv.",
        "Shikoni «{0}» dhe zgjidhni një rresht për veprime.",
        "Shikoni «{0}» dhe zgjidhni një element.",
        "Hapni skedën «{0}» për cilësimet ose informacionin.",
        "Vendosni fjalëkalimin për «{0}»; karakteret fshihen.",
        "Rregulloni «{0}» në vlerën e dëshiruar.",
        "Informacion dhe veprime për këtë kontroll.",
        "këtë cilësim",
    ),
    "ar": (
        "اختر «{0}» لتنفيذ هذا الإجراء.",
        "أدخل أو عدّل «{0}» في النموذج الحالي.",
        "اختر «{0}» من الخيارات المتاحة.",
        "فعّل أو عطّل «{0}» في هذه اللوحة.",
        "اختر «{0}» بوصفه الخيار النشط.",
        "راجع «{0}» وحدد صفًا للإجراءات المتاحة.",
        "راجع «{0}» وحدد عنصرًا.",
        "افتح علامة التبويب «{0}» للإعدادات أو المعلومات.",
        "أدخل كلمة المرور لـ «{0}»؛ الأحرف مخفية.",
        "اضبط «{0}» على القيمة المطلوبة.",
        "معلومات وإجراءات متاحة لهذا العنصر.",
        "هذا الإعداد",
    ),
    "da": (
        "Vælg «{0}» for at udføre denne handling.",
        "Indtast eller rediger «{0}» i formularen.",
        "Vælg «{0}» blandt de tilgængelige muligheder.",
        "Aktivér eller deaktivér «{0}» i dette panel.",
        "Vælg «{0}» som aktiv indstilling.",
        "Gennemse «{0}», og vælg en række for mulige handlinger.",
        "Gennemse «{0}», og vælg et element.",
        "Åbn fanen «{0}» for indstillinger eller oplysninger.",
        "Indtast adgangskoden til «{0}»; tegnene skjules.",
        "Juster «{0}» til den ønskede værdi.",
        "Oplysninger og tilgængelige handlinger for dette felt.",
        "denne indstilling",
    ),
    "de": (
        "Wählen Sie «{0}», um diese Aktion auszuführen.",
        "Geben Sie «{0}» in diesem Formular ein oder bearbeiten Sie es.",
        "Wählen Sie «{0}» aus den verfügbaren Optionen.",
        "Aktivieren oder deaktivieren Sie «{0}» in diesem Bereich.",
        "Wählen Sie «{0}» als aktive Option.",
        "Prüfen Sie «{0}» und wählen Sie eine Zeile für weitere Aktionen.",
        "Prüfen Sie «{0}» und wählen Sie einen Eintrag.",
        "Öffnen Sie den Reiter «{0}» für Einstellungen und Informationen.",
        "Geben Sie das Kennwort für «{0}» ein; Zeichen bleiben verborgen.",
        "Stellen Sie «{0}» auf den gewünschten Wert.",
        "Informationen und verfügbare Aktionen für dieses Steuerelement.",
        "diese Einstellung",
    ),
    "el": (
        "Επιλέξτε «{0}» για να εκτελέσετε αυτή την ενέργεια.",
        "Εισαγάγετε ή επεξεργαστείτε «{0}» στη φόρμα.",
        "Επιλέξτε «{0}» από τις διαθέσιμες επιλογές.",
        "Ενεργοποιήστε ή απενεργοποιήστε «{0}» σε αυτόν τον πίνακα.",
        "Επιλέξτε «{0}» ως ενεργή επιλογή.",
        "Ελέγξτε «{0}» και επιλέξτε γραμμή για ενέργειες.",
        "Ελέγξτε «{0}» και επιλέξτε στοιχείο.",
        "Ανοίξτε την καρτέλα «{0}» για ρυθμίσεις ή πληροφορίες.",
        "Πληκτρολογήστε τον κωδικό για «{0}»· οι χαρακτήρες κρύβονται.",
        "Ρυθμίστε «{0}» στην επιθυμητή τιμή.",
        "Πληροφορίες και διαθέσιμες ενέργειες για αυτό το στοιχείο.",
        "αυτή τη ρύθμιση",
    ),
    "en_US": None,
    "es": (
        "Pulse «{0}» para realizar esta operación.",
        "Capture o edite «{0}» en este formulario.",
        "Seleccione «{0}» de las opciones disponibles.",
        "Active o desactive «{0}» en este panel.",
        "Marque «{0}» como opción activa.",
        "Consulte «{0}» y seleccione una fila para las acciones disponibles.",
        "Consulte «{0}» y seleccione un elemento.",
        "Abra la pestaña «{0}» para acceder a sus ajustes o información.",
        "Introduzca la contraseña de «{0}»; los caracteres permanecerán ocultos.",
        "Ajuste «{0}» al valor deseado.",
        "Información y acciones disponibles para este control.",
        "esta configuración",
    ),
    "es_AR": None,
    "es_MX": None,
    "et": (
        "Valige «{0}», et see toiming teha.",
        "Sisestage või muutke vormil välja «{0}».",
        "Valige «{0}» saadaolevate valikute hulgast.",
        "Lülitage «{0}» sellel paneelil sisse või välja.",
        "Märkige «{0}» aktiivseks valikuks.",
        "Vaadake «{0}» ja valige rida toiminguteks.",
        "Vaadake «{0}» ja valige üksus.",
        "Avage vahekaart «{0}» seadete või teabe jaoks.",
        "Sisestage «{0}» parool; märgid on peidetud.",
        "Seadistage «{0}» soovitud väärtusele.",
        "Selle juhtelemendi teave ja saadaolevad toimingud.",
        "see seadistus",
    ),
    "fr": (
        "Sélectionnez «{0}» pour effectuer cette opération.",
        "Saisissez ou modifiez «{0}» dans ce formulaire.",
        "Choisissez «{0}» parmi les options disponibles.",
        "Activez ou désactivez «{0}» dans ce panneau.",
        "Sélectionnez «{0}» comme option active.",
        "Consultez «{0}» et sélectionnez une ligne pour les actions possibles.",
        "Consultez «{0}» et choisissez un élément.",
        "Ouvrez l’onglet «{0}» pour ses paramètres ou informations.",
        "Saisissez le mot de passe de «{0}» ; les caractères sont masqués.",
        "Réglez «{0}» à la valeur souhaitée.",
        "Informations et actions disponibles pour ce contrôle.",
        "ce paramètre",
    ),
    "hr": (
        "Odaberite «{0}» za izvršavanje ove radnje.",
        "Unesite ili uredite «{0}» u obrascu.",
        "Odaberite «{0}» među dostupnim mogućnostima.",
        "Omogućite ili onemogućite «{0}» na ovoj ploči.",
        "Odaberite «{0}» kao aktivnu mogućnost.",
        "Pregledajte «{0}» i odaberite redak za dostupne radnje.",
        "Pregledajte «{0}» i odaberite stavku.",
        "Otvorite karticu «{0}» za postavke ili informacije.",
        "Unesite lozinku za «{0}»; znakovi su skriveni.",
        "Postavite «{0}» na željenu vrijednost.",
        "Informacije i dostupne radnje za ovu kontrolu.",
        "ovu postavku",
    ),
    "it": (
        "Seleziona «{0}» per eseguire questa operazione.",
        "Inserisci o modifica «{0}» in questo modulo.",
        "Scegli «{0}» tra le opzioni disponibili.",
        "Attiva o disattiva «{0}» in questo pannello.",
        "Seleziona «{0}» come opzione attiva.",
        "Consulta «{0}» e seleziona una riga per le azioni disponibili.",
        "Consulta «{0}» e seleziona un elemento.",
        "Apri la scheda «{0}» per impostazioni o informazioni.",
        "Inserisci la password per «{0}»; i caratteri rimangono nascosti.",
        "Imposta «{0}» sul valore desiderato.",
        "Informazioni e azioni disponibili per questo controllo.",
        "questa impostazione",
    ),
    "nl": (
        "Selecteer «{0}» om deze handeling uit te voeren.",
        "Voer «{0}» in of bewerk deze waarde in het formulier.",
        "Kies «{0}» uit de beschikbare opties.",
        "Schakel «{0}» in of uit op dit paneel.",
        "Selecteer «{0}» als actieve optie.",
        "Bekijk «{0}» en selecteer een rij voor beschikbare acties.",
        "Bekijk «{0}» en kies een item.",
        "Open het tabblad «{0}» voor instellingen of informatie.",
        "Voer het wachtwoord voor «{0}» in; tekens blijven verborgen.",
        "Stel «{0}» in op de gewenste waarde.",
        "Informatie en beschikbare acties voor dit besturingselement.",
        "deze instelling",
    ),
    "pt": (
        "Selecione «{0}» para executar esta operação.",
        "Introduza ou edite «{0}» neste formulário.",
        "Escolha «{0}» entre as opções disponíveis.",
        "Ative ou desative «{0}» neste painel.",
        "Selecione «{0}» como opção ativa.",
        "Consulte «{0}» e selecione uma linha para as ações disponíveis.",
        "Consulte «{0}» e selecione um elemento.",
        "Abra o separador «{0}» para as definições ou informações.",
        "Introduza a palavra-passe de «{0}»; os caracteres ficam ocultos.",
        "Ajuste «{0}» para o valor desejado.",
        "Informações e ações disponíveis para este controlo.",
        "esta definição",
    ),
    "pt_BR": (
        "Selecione «{0}» para executar esta operação.",
        "Digite ou edite «{0}» neste formulário.",
        "Escolha «{0}» entre as opções disponíveis.",
        "Ative ou desative «{0}» neste painel.",
        "Selecione «{0}» como opção ativa.",
        "Consulte «{0}» e selecione uma linha para as ações disponíveis.",
        "Consulte «{0}» e escolha um item.",
        "Abra a aba «{0}» para configurações ou informações.",
        "Digite a senha de «{0}»; os caracteres ficam ocultos.",
        "Ajuste «{0}» para o valor desejado.",
        "Informações e ações disponíveis para este controle.",
        "esta configuração",
    ),
}
PHRASES["en_US"]=PHRASES["base"]
PHRASES["es_AR"]=PHRASES["es"]
PHRASES["es_MX"]=PHRASES["es"]

FORMATS=("button","input","select","check","radio","table","list","tab","password","range")
FIELDS=re.compile(r"\b((?:javax\.swing\.)?(?:JButton|JTextField|JComboBox|JCheckBox|JRadioButton|JTable|JList|JSpinner|JPasswordField|JTabbedPane|JToggleButton|JMenuItem|JTextArea|JFormattedTextField))\s*(?:<[^>]+>)?\s+(\w+)\s*[;=]")
PREFIXES=re.compile(r"^(?:m_j|j|web|m_|txt|btn|cbo|cmb|chk|lbl|jtxt|jbtn|jcbo|jchk|jlbl|m_jtxt|m_jbtn|m_jcbo)",re.I)

# Dedicated user-facing messages: include both defaults and business-critical controls.
SPECIAL = {
    "scale.request": [
        "Select the scale's weight request command. AUTO keeps the driver default; Torrey uses P, CAS W/W+CR, Mettler S/SI and others may need custom ASCII or HEX.",
        "Seleccione el comando con el que solicita peso la báscula. AUTO conserva el driver; Torrey P, CAS W/W+CR, Mettler S/SI; admite ASCII o HEX personalizados.",
        "Wählen Sie das Kommando zur Gewichtsabfrage. AUTO nutzt den Treiberstandard; ASCII- oder HEX-Befehle sind möglich.",
        "Sélectionnez la commande de lecture du poids. AUTO utilise le pilote par défaut ; les commandes ASCII ou HEX sont acceptées.",
        "Selecione o comando para solicitar o peso. AUTO mantém o valor do driver; também aceita comandos ASCII ou HEX."
    ],
    "platforms.new": [
        "Enter the name of a new ordering platform; it will become available when saved.",
        "Escriba el nombre de una nueva plataforma de pedidos; estará disponible al guardar.",
        "Geben Sie den Namen einer neuen Bestellplattform ein; beim Speichern wird sie verfügbar.",
        "Saisissez le nom d'une nouvelle plateforme de commande, ajoutée après enregistrement.",
        "Digite o nome de uma nova plataforma de pedidos, adicionada ao salvar."
    ],
    "platforms.add": [
        "Add the entered platform to the list of additional ordering services.",
        "Agrega la plataforma escrita a la lista de servicios adicionales de pedidos.",
        "Fügen Sie die eingegebene Plattform zur Liste weiterer Bestelldienste hinzu.",
        "Ajoute la plateforme saisie à la liste des services de commande supplémentaires.",
        "Adiciona a plataforma informada à lista de serviços adicionais de pedidos."
    ],
    "platforms.remove": [
        "Remove the selected custom platform; built-in providers cannot be deleted.",
        "Elimina la plataforma personalizada seleccionada; no se pueden borrar las incluidas.",
        "Entfernt die ausgewählte eigene Plattform; integrierte Anbieter bleiben erhalten.",
        "Supprime la plateforme personnalisée sélectionnée ; les services intégrés restent.",
        "Remove a plataforma personalizada selecionada; as plataformas integradas permanecem."
    ],
    "peripheral.digitalpath": [
        "Enter the destination folder for copies of digital receipts; use a writable path.",
        "Indique la carpeta donde se guardarán las copias digitales de tickets; debe permitir escritura.",
        "Geben Sie den Ordner für digitale Belegkopien an; er muss beschreibbar sein.",
        "Indiquez le dossier de copie des reçus numériques ; il doit être accessible en écriture.",
        "Informe a pasta das cópias digitais dos comprovantes; ela deve permitir gravação."
    ],
    "peripheral.mirror": [
        "Enable an additional receipt output. The main receipt printer stays unchanged.",
        "Activa una salida adicional del ticket sin modificar la impresora principal.",
        "Aktiviert eine zusätzliche Belegausgabe ohne Änderung des Hauptdruckers.",
        "Active une sortie supplémentaire de reçu sans modifier l'imprimante principale.",
        "Ativa uma saída adicional do comprovante sem alterar a impressora principal."
    ],
    "order.code": [
        "Enter the platform order identifier. Required when platform code validation is enabled.",
        "Capture el identificador del pedido de plataforma; es obligatorio si está habilitada su validación.",
        "Geben Sie die Bestellnummer der Plattform ein; bei aktivierter Prüfung ist sie erforderlich.",
        "Saisissez l'identifiant de commande de la plateforme ; requis si le contrôle est actif.",
        "Digite o código do pedido da plataforma; obrigatório quando a validação está ativa."
    ],
    "sales.preview": [
        "Preview the current ticket without completing the sale or collecting payment.",
        "Muestra una vista previa del ticket actual sin cerrar la venta ni registrar el cobro.",
        "Zeigt den aktuellen Bon an, ohne den Verkauf abzuschließen oder die Zahlung zu buchen.",
        "Affiche un aperçu du reçu actuel sans terminer la vente ni encaisser.",
        "Visualiza o comprovante atual sem finalizar a venda nem registrar o pagamento."
    ],
    "sales.payment": [
        "Open payment selection for the current ticket; confirm amounts before closing.",
        "Abre las formas de cobro del ticket; compruebe los importes antes de finalizar.",
        "Öffnet die Zahlungsarten des Bons; prüfen Sie die Beträge vor Abschluss.",
        "Ouvre les moyens de paiement du reçu ; vérifiez les montants avant validation.",
        "Abre os meios de pagamento do cupom; confirme os valores antes de finalizar."
    ],
    "report.filter": [
        "Set the report filters and date range; run the report to refresh the results.",
        "Configure los filtros y fechas del informe; ejecútelo para actualizar los resultados.",
        "Stellen Sie Filter und Datumsbereich des Berichts ein und führen Sie ihn erneut aus.",
        "Définissez les filtres et dates du rapport ; exécutez-le pour actualiser les résultats.",
        "Defina filtros e datas do relatório; execute novamente para atualizar os resultados."
    ],
}
LANG_INDEX={
    "base":0, "en_US":0,
    "es":1,"es_AR":1,"es_MX":1,
    "de":2,"fr":3,"pt":4,"pt_BR":4,
}
UI_CAPTIONS={
    "platforms.included": ("Built-in platforms (fixed)","Plataformas incluidas (fijas)"),
    "platforms.note": ("Built-in platforms are always available and cannot be removed.","Las plataformas incluidas siempre estarán disponibles y no se pueden eliminar."),
    "platforms.custom": ("Additional platforms","Plataformas adicionales"),
    "platforms.newlabel": ("New platform:","Nueva plataforma:"),
    "platforms.add": ("Add","Agregar"),
    "platforms.remove": ("Remove selected","Eliminar seleccionada"),
    "platforms.error.required": ("Platform name is required.","El nombre de la plataforma es obligatorio."),
    "platforms.error.separator": ("Platform name cannot contain the | character.","El nombre de la plataforma no puede contener el carácter |."),
    "platforms.error.duplicate": ("This platform already exists.","La plataforma ya existe."),
    "platforms.title": ("Platforms","Plataformas"),
    "scale.request.label": ("Request:","Solicitud:"),
    "nodes.reload": ("Configuration reloaded from disk.","Configuración recargada desde disco"),
    "nodes.readonly": ("Read-only: this node is not the master.","Modo consulta: este nodo no tiene rol master"),
    "nodes.empty": ("The master has no registered nodes.","El MASTER no contiene nodos registrados"),
    "nodes.remotepath": ("Managed by remote master","Gestionados por MASTER remoto"),
}
# Targeted translations where the existing POS catalog does not already provide one.
ADDITIONAL={
"de":{"platforms.included":"Enthaltene Plattformen (fest)","platforms.note":"Enthaltene Plattformen sind immer verfügbar und können nicht entfernt werden.","platforms.custom":"Weitere Plattformen","platforms.newlabel":"Neue Plattform:","platforms.add":"Hinzufügen","platforms.remove":"Auswahl entfernen","platforms.title":"Plattformen","scale.request.label":"Anfrage:"},
"fr":{"platforms.included":"Plateformes intégrées (fixes)","platforms.note":"Les plateformes intégrées sont toujours disponibles et ne peuvent pas être supprimées.","platforms.custom":"Plateformes supplémentaires","platforms.newlabel":"Nouvelle plateforme :","platforms.add":"Ajouter","platforms.remove":"Supprimer la sélection","platforms.title":"Plateformes","scale.request.label":"Demande :"},
"it":{"platforms.included":"Piattaforme incluse (fisse)","platforms.note":"Le piattaforme incluse sono sempre disponibili e non possono essere rimosse.","platforms.custom":"Piattaforme aggiuntive","platforms.newlabel":"Nuova piattaforma:","platforms.add":"Aggiungi","platforms.remove":"Rimuovi selezionata","platforms.title":"Piattaforme","scale.request.label":"Richiesta:"},
"pt":{"platforms.included":"Plataformas incluídas (fixas)","platforms.note":"As plataformas incluídas estão sempre disponíveis e não podem ser removidas.","platforms.custom":"Plataformas adicionais","platforms.newlabel":"Nova plataforma:","platforms.add":"Adicionar","platforms.remove":"Remover selecionada","platforms.title":"Plataformas","scale.request.label":"Pedido:"},
"pt_BR":{"platforms.included":"Plataformas incluídas (fixas)","platforms.note":"As plataformas incluídas estão sempre disponíveis e não podem ser removidas.","platforms.custom":"Plataformas adicionais","platforms.newlabel":"Nova plataforma:","platforms.add":"Adicionar","platforms.remove":"Remover selecionada","platforms.title":"Plataformas","scale.request.label":"Solicitação:"},
"da":{"platforms.included":"Indbyggede platforme (faste)","platforms.note":"Indbyggede platforme er altid tilgængelige og kan ikke fjernes.","platforms.custom":"Ekstra platforme","platforms.newlabel":"Ny platform:","platforms.add":"Tilføj","platforms.remove":"Fjern valgte","platforms.title":"Platforme","scale.request.label":"Forespørgsel:"},
"nl":{"platforms.included":"Ingebouwde platforms (vast)","platforms.note":"Ingebouwde platforms blijven beschikbaar en kunnen niet worden verwijderd.","platforms.custom":"Extra platforms","platforms.newlabel":"Nieuw platform:","platforms.add":"Toevoegen","platforms.remove":"Selectie verwijderen","platforms.title":"Platforms","scale.request.label":"Verzoek:"},
"ar":{"platforms.included":"المنصات المضمنة (ثابتة)","platforms.note":"المنصات المضمنة متاحة دائمًا ولا يمكن حذفها.","platforms.custom":"منصات إضافية","platforms.newlabel":"منصة جديدة:","platforms.add":"إضافة","platforms.remove":"حذف المحدد","platforms.title":"المنصات","scale.request.label":"طلب:"},
"el":{"platforms.included":"Ενσωματωμένες πλατφόρμες (σταθερές)","platforms.note":"Οι ενσωματωμένες πλατφόρμες είναι πάντα διαθέσιμες και δεν διαγράφονται.","platforms.custom":"Πρόσθετες πλατφόρμες","platforms.newlabel":"Νέα πλατφόρμα:","platforms.add":"Προσθήκη","platforms.remove":"Αφαίρεση επιλεγμένης","platforms.title":"Πλατφόρμες","scale.request.label":"Αίτημα:"},
"et":{"platforms.included":"Sisseehitatud platvormid (püsivad)","platforms.note":"Sisseehitatud platvormid on alati saadaval ja neid ei saa eemaldada.","platforms.custom":"Lisaplatvormid","platforms.newlabel":"Uus platvorm:","platforms.add":"Lisa","platforms.remove":"Eemalda valitud","platforms.title":"Platvormid","scale.request.label":"Päring:"},
"hr":{"platforms.included":"Uključene platforme (trajne)","platforms.note":"Uključene platforme uvijek su dostupne i ne mogu se ukloniti.","platforms.custom":"Dodatne platforme","platforms.newlabel":"Nova platforma:","platforms.add":"Dodaj","platforms.remove":"Ukloni odabrano","platforms.title":"Platforme","scale.request.label":"Zahtjev:"},
"al_SQ":{"platforms.included":"Platformat e përfshira (fikse)","platforms.note":"Platformat e përfshira janë gjithmonë të disponueshme dhe nuk hiqen.","platforms.custom":"Platforma shtesë","platforms.newlabel":"Platformë e re:","platforms.add":"Shto","platforms.remove":"Hiq të zgjedhurën","platforms.title":"Platformat","scale.request.label":"Kërkesë:"},
}
# Fixed sales-configuration captions. All existing POS locales have native text.
# Order: platform, scale, remoteprint, split, reprint, preview, note, title.
SALES_BUTTONS={
 "base":("Order code / platforms","Scale","Remote printing / Kitchen Screen","Split bill","Print last receipt","Print / preview current receipt","<html>Select which optional buttons appear during a sale.<br>User permissions are still enforced by roles.</html>","Optional buttons"),
 "al_SQ":("Kodi i porosisë / platformat","Peshorja","Printim në distancë / Kitchen Screen","Ndaj faturën","Printo kuponin e fundit","Printo / shiko kuponin aktual","<html>Zgjidhni butonat opsionalë në ekranin e shitjes.<br>Lejet e përdoruesit vazhdojnë të kontrollohen nga rolet.</html>","Butonat opsionalë"),
 "ar":("رمز الطلب / المنصات","الميزان","الطباعة عن بُعد / شاشة المطبخ","تقسيم الفاتورة","طباعة آخر إيصال","طباعة / معاينة الإيصال الحالي","<html>اختر الأزرار الاختيارية المعروضة عند البيع.<br>تظل صلاحيات المستخدم خاضعة للأدوار.</html>","الأزرار الاختيارية"),
 "da":("Ordrekode / platforme","Vægt","Fjernudskrift / Kitchen Screen","Opdel regning","Udskriv sidste bon","Udskriv / vis aktuel bon","<html>Vælg de ekstra knapper på salgsskærmen.<br>Brugertilladelser styres fortsat af roller.</html>","Ekstra knapper"),
 "de":("Bestellcode / Plattformen","Waage","Ferndruck / Kitchen Screen","Rechnung teilen","Letzten Bon drucken","Aktuellen Bon drucken / Vorschau","<html>Wählen Sie die optionalen Schaltflächen der Verkaufsansicht.<br>Benutzerrechte werden weiterhin über Rollen gesteuert.</html>","Optionale Schaltflächen"),
 "el":("Κωδικός παραγγελίας / πλατφόρμες","Ζυγαριά","Απομακρυσμένη εκτύπωση / Kitchen Screen","Διαχωρισμός λογαριασμού","Εκτύπωση τελευταίας απόδειξης","Εκτύπωση / προεπισκόπηση τρέχουσας απόδειξης","<html>Επιλέξτε τα προαιρετικά κουμπιά της οθόνης πωλήσεων.<br>Τα δικαιώματα χρήστη εξακολουθούν να ελέγχονται από τους ρόλους.</html>","Προαιρετικά κουμπιά"),
 "en_US":None,
 "es":("Código de orden / plataformas","Báscula","Impresión remota / Kitchen Screen","Dividir cuenta","Imprimir último ticket","Imprimir / previsualizar ticket actual","<html>Seleccione los botones opcionales de la pantalla de ventas.<br>Los permisos de usuario continúan controlándose mediante roles.</html>","Botones opcionales"),
 "es_AR":None,"es_MX":None,
 "et":("Tellimuse kood / platvormid","Kaal","Kaugprintimine / Kitchen Screen","Jaga arve","Prindi viimane kviitung","Prindi / eelvaata praegust kviitungit","<html>Valige müügivaate valikulised nupud.<br>Kasutajaõigusi juhivad endiselt rollid.</html>","Valikulised nupud"),
 "fr":("Code de commande / plateformes","Balance","Impression distante / Kitchen Screen","Partager l'addition","Imprimer le dernier reçu","Imprimer / prévisualiser le reçu actuel","<html>Choisissez les boutons facultatifs de la vente.<br>Les droits des utilisateurs restent gérés par les rôles.</html>","Boutons facultatifs"),
 "hr":("Šifra narudžbe / platforme","Vaga","Udaljeni ispis / Kitchen Screen","Podijeli račun","Ispiši zadnji račun","Ispiši / pregledaj trenutačni račun","<html>Odaberite dodatne gumbe na prodajnom zaslonu.<br>Korisnička prava i dalje određuju uloge.</html>","Dodatni gumbi"),
 "it":("Codice ordine / piattaforme","Bilancia","Stampa remota / Kitchen Screen","Dividi conto","Stampa ultima ricevuta","Stampa / anteprima ricevuta attuale","<html>Seleziona i pulsanti facoltativi della vendita.<br>I permessi degli utenti restano gestiti dai ruoli.</html>","Pulsanti facoltativi"),
 "nl":("Bestelcode / platforms","Weegschaal","Afdrukken op afstand / Kitchen Screen","Rekening splitsen","Laatste bon afdrukken","Huidige bon afdrukken / voorbeeld","<html>Kies de optionele knoppen op het verkoopscherm.<br>Gebruikersrechten blijven bepaald door rollen.</html>","Optionele knoppen"),
 "pt":("Código da encomenda / plataformas","Balança","Impressão remota / Kitchen Screen","Dividir conta","Imprimir último recibo","Imprimir / pré-visualizar recibo atual","<html>Selecione os botões opcionais do ecrã de venda.<br>As permissões continuam a ser controladas por funções.</html>","Botões opcionais"),
 "pt_BR":("Código do pedido / plataformas","Balança","Impressão remota / Kitchen Screen","Dividir conta","Imprimir último comprovante","Imprimir / visualizar comprovante atual","<html>Selecione os botões opcionais da tela de vendas.<br>As permissões continuam sendo controladas por funções.</html>","Botões opcionais"),
}
SALES_BUTTONS["en_US"]=SALES_BUTTONS["base"]
SALES_BUTTONS["es_AR"]=SALES_BUTTONS["es"]
SALES_BUTTONS["es_MX"]=SALES_BUTTONS["es"]
SALES_KEYS=("platform","scale","remoteprint","split","reprint","preview","note","title")

GLOSSARY={
"name":"label.name","code":"label.code","barcode":"label.prodbarcode","price":"label.price",
"tax":"label.tax","category":"label.prodcategory","quantity":"label.units","units":"label.units",
"country":"label.country","city":"label.city","phone":"label.phone","email":"label.email",
"address":"label.address","password":"label.password","customer":"label.customer",
"printer":"label.machineprinter","scale":"label.scale","scanner":"label.scanner",
"stock":"label.stock","ticket":"label.ticket","amount":"label.totalcash",
"date":"label.date","discount":"label.discount","user":"label.user",
"purchase":"label.pricebuy","product":"label.stockproduct","reference":"label.prodref",
"location":"label.location","role":"label.role","supplier":"label.supplier",
}
@__import__("functools").lru_cache(maxsize=None)
def read_bundle(path:Path)->dict[str,str]:
    rows={}
    if path.exists():
        for line in path.read_text(encoding="latin1").splitlines():
            if "=" in line and not line.startswith(("#","!")):
                k,v=line.split("=",1)
                value=v.strip()
                # NetBeans bundles are ISO-8859-1 with Java \uNNNN escapes.
                # Decode the full Unicode range, not only ñ and é.
                value=re.sub(r"(?<!\\)\\u([0-9a-fA-F]{4})",
                    lambda m:chr(int(m.group(1),16)),value)
                rows[k.strip()]=value
    return rows
def escape(v:str)->str:
    # Java Properties with UTF-8 Reader: escape only syntax delimiters and \n.
    return v.replace("\\",r"\\").replace("\r","").replace("\n",r"\n")
def safe_text(value:str)->str:
    return re.sub(r"\s+"," ",value).strip()
def derive(name:str)->str:
    # NetBeans names mix case (jBtnFind, jButtonFind, m_jcboDevice,
    # jCboDevice). Strip the *longest* UI prefix before tokenization, or
    # labels exposed to the user become nonsense such as "btn / cbo / chk".
    prefixes=("m_jtextfield","m_jcombobox","m_jpasswordfield",
              "m_jbutton","m_jcombo","m_jcheck","m_jlabel",
              "m_jtextarea","m_jradiobutton","m_jcheckbox",
              "m_jtxt","m_jcbo","m_jbtn","m_jchk","m_jlbl",
              "jtextfield","jcombobox","jpasswordfield","jformattedtextfield",
              "jtabbedpane","jradiobutton","jcheckbox","jcombo",
              "jcheck","jlabel","jtextarea",
              "jtogglebutton","jmenuitem","jbutton","jspinner",
              "jtxt","jbtn","jcbo","jchk","jlbl","jcmd",
              "m_j","web","txt","btn","cbo","cmb","chk","lbl","j")
    lower=name.lower()
    for prefix in prefixes:
        if lower.startswith(prefix) and len(name)>len(prefix):
            name=name[len(prefix):]
            break
    name=re.sub(r"([a-z])([A-Z])",r"\1 \2",name)
    name=re.sub(r"([A-Z])([A-Z][a-z])",r"\1 \2",name)
    name=re.sub(r"[0-9]+$","",name)
    tokens=safe_text(name).lower().split()
    return " ".join(tokens) or "setting"
def localized_vocab(locale:str)->dict[str,str]:
    loc=read_bundle(ROOT/f"src/main/resources/pos_messages_{locale}.properties") if locale!="base" else {}
    fallback=read_bundle(ROOT/"src/main/resources/pos_messages.properties")
    inherited={token:safe_text(loc.get(key,fallback.get(key,token)))
               for token,key in GLOSSARY.items()}
    inherited.update(LOCALIZED_TERMS[locale])
    return inherited

def contextual_semantic(owner,name):
    label=derive(name)
    context=BUSINESS_FIELD_CONTEXTS.get(owner)
    if context is not None and label in ("name","text","list","table","label"):
        return label+" ("+context+")"
    return label

def collect_controls():
    out={}
    for f in sorted(JAVA.rglob("*.java")):
        code=f.read_text(encoding="utf-8",errors="replace")
        match=re.search(r"\bpackage\s+([\w.]+)\s*;",code)
        if not match:continue
        package=match.group(1)
        cls=f.stem
        for component,name in FIELDS.findall(code):
            fq=package+"."+cls+"#"+name
            out[fq]=(component.rsplit(".",1)[-1],contextual_semantic(package+"."+cls,name))
        # Clickable JLabel images and links are actions too. Do not collect
        # every static caption: only labels with an explicit mouse listener.
        for component,name in re.findall(
                r"\b((?:javax\.swing\.)?JLabel)\s+(\w+)\s*[;=]",code):
            if re.search(r"\b"+re.escape(name)+r"\.addMouseListener\s*\(",code):
                out[package+"."+cls+"#"+name]=("JLabel",contextual_semantic(package+"."+cls,name))
    return out

@__import__("functools").lru_cache(maxsize=1)
def collect_legacy_resource_hints():
    # Copy proven user-facing messages from the existing POS bundles into the
    # sealed UI-help JAR. Never load these legacy files at runtime.
    source={}
    for file in sorted(JAVA.rglob("*.java")):
        text=file.read_text(encoding="utf-8",errors="replace")
        match=re.search(r"\bpackage\s+([\w.]+)\s*;",text)
        if not match:continue
        owner=match.group(1)+"."+file.stem
        fields={name for _,name in FIELDS.findall(text)}
        fields.update(name for _,name in re.findall(
            r"\b((?:javax\.swing\.)?JLabel)\s+(\w+)\s*[;=]",text)
            if re.search(r"\b"+re.escape(name)+r"\.addMouseListener\s*\(",text))
        for field in fields:
            for action in ("setToolTipText","setText"):
                pattern=(r"\b"+re.escape(field)+r"\."+action+
                    r'\s*\(\s*(?:AppLocal\.getIntString|bundle\.getString)\s*\(\s*"([^"]+)"')
                match=re.search(pattern,text)
                if match:
                    source[owner+"#"+field]=(action,match.group(1))
                    break
    return source


def native_resource_message(locale,key):
    # A locale never silently inherits an English tooltip if no native string
    # exists; the native UI-help sentence remains the fallback instead.
    chains={"base":("base",),"en_US":("en_US","base"),
        "es_MX":("es_MX","es"),"es_AR":("es_AR","es"),
        "pt_BR":("pt_BR","pt")}
    search=chains.get(locale,(locale,))
    for candidate in search:
        path=ROOT/("src/main/resources/pos_messages.properties" if candidate=="base"
            else f"src/main/resources/pos_messages_{candidate}.properties")
        value=read_bundle(path).get(key)
        if value and value.strip():
            # Skip highly structured HTML/format strings; those require
            # dedicated, curated semantic entries to avoid malformed markup.
            if value.lstrip().startswith("<html") or "\n" in value:
                return None
            # Java properties use ASCII escapes for diacritics.
            value=re.sub(r"\\u([0-9a-fA-F]{4})",
                lambda m:chr(int(m.group(1),16)),value)
            if len(value)>200 or value.startswith(("jdbc:","http://","https://")):
                return None
            return safe_text(value)
    return None


def collect_literals():
    literals=set()
    for file in JAVA.rglob("*.java"):
        text=file.read_text(encoding="utf-8",errors="replace")
        patterns=(
            r'new\s+(?:javax\.swing\.)?(?:JLabel|JButton|JCheckBox|JRadioButton)\s*\(\s*"((?:\\.|[^"\\])*)"',
            r'\b(?:setText|setTitle|createTitledBorder)\s*\(\s*"((?:\\.|[^"\\])*)"',
        )
        for pattern in patterns:
            for literal in re.findall(pattern,text):
                if (3<=len(literal)<=105 and not re.match(r"^(?:jdbc:|https?://)",literal,re.I)
                        and not re.fullmatch(r"[\d\.%+-]+",literal)
                        and not re.fullmatch(r"(?:jButton|jLabel|jTextField|webTextField)\d+",literal)
                        and "\\" not in literal):
                    literals.add(literal)
    return literals

def guess_literal(locale,original,base,translated):
    # Existing POS resource files occasionally use the same short word with
    # unrelated meanings (e.g. PIN/Card or Available/Exists). Never guess
    # translations from unrelated keys. Only use curated literal mappings.
    explicit=TRANSLATIONS.get(original)
    return explicit[locale] if explicit is not None else original

def write_one(locale, controls, literals):
    strings=PHRASES[locale];assert len(strings)==12
    out={f"format.{kind}":strings[i] for i,kind in enumerate(FORMATS)}
    out["text.common.help"]=strings[10]
    for k in FORMATS:out["target."+k]=strings[11]
    for k,translated in ADDITIONAL.get(locale,{}).items():
        out["text."+k]=translated
    out.update(LOCALIZED_HTML_TEXTS[locale])
    out.update(LOCALIZED_CONFIG_TABS[locale])
    out.update(LOCALIZED_PRODUCT_EDITOR[locale])
    out.update(LOCALIZED_CONFIG_ACTIONS[locale])
    out.update(LOCALIZED_NODE_ROLES[locale])
    out.update(LOCALIZED_NODE_FORM[locale])
    out.update(LOCALIZED_REPORT_CONTROLS[locale])
    out.update(DATABASE_ACCOUNT_HELP[locale])
    for key in TAB_KEYS:
        label=out["text.config.tab."+key]
        out["tooltip.config.tab."+key]=strings[7].replace("{0}",label)
    for k,(english,spanish) in UI_CAPTIONS.items():
        out.setdefault("text."+k,spanish if locale in ("es","es_AR","es_MX") else english)
    for key,value in zip(PLATFORM_KEYS,PLATFORM_ERRORS[locale]):
        out["text.platforms."+key]=value
    for key,value in zip(NODE_KEYS,NODE_TEXTS[locale]):
        out["text.nodes."+key]=value
    for key,caption in zip(SALES_KEYS,SALES_BUTTONS[locale]):
        out["text.salesbuttons."+key]=caption
        if key not in ("note","title"):
            out["tooltip.salesbuttons."+key]=strings[3].replace("{0}",caption)
    out["tooltip.platforms.list"]=strings[6].replace("{0}",out["text.platforms.custom"])
    for key,options in SPECIAL.items():
        if locale in LANG_INDEX:
            out["tooltip."+key]=options[LANG_INDEX[locale]]
        elif key=="scale.request":
            out["tooltip."+key]=strings[2].replace("{0}",out["text.scale.request.label"])+" AUTO / ASCII / HEX / CAS / Torrey / Mettler."
        elif key.startswith("platforms."):
            out["tooltip."+key]=strings[0].replace("{0}",out["text.platforms.title"])
        elif key.startswith("peripheral."):
            out["tooltip."+key]=strings[2].replace("{0}",out.get("term.printer","Printer"))
        elif key.startswith("order."):
            out["tooltip."+key]=strings[1].replace("{0}",out.get("term.code","order code"))
        elif key.startswith("report."):
            out["tooltip."+key]=strings[2].replace("{0}",out.get("term.date","date"))
        else:
            out["tooltip."+key]=strings[0].replace("{0}",out.get("term.ticket","receipt"))
    base=read_bundle(ROOT/"src/main/resources/pos_messages.properties")
    loc=read_bundle(ROOT/f"src/main/resources/pos_messages_{locale}.properties")
    for token,value in localized_vocab(locale).items():
        out["term."+token]=value
    for text in sorted(literals):
        digest=hashlib.sha256(text.encode()).hexdigest()[:16]
        out["literal."+digest]=guess_literal(locale,text,base,loc)
    legacy_hints=collect_legacy_resource_hints()
    # Recover static English descriptions from actual Swing code; never
    # translate arbitrary data or unreviewed literals.
    for java in sorted(JAVA.rglob("*.java")):
        code=java.read_text(encoding="utf-8",errors="replace")
        package_match=re.search(r"\bpackage\s+([\w.]+)\s*;",code)
        if not package_match: continue
        owner=package_match.group(1)+"."+java.stem
        for variable,caption in re.findall(
                r'\b(\w+)\.setToolTipText\s*\(\s*"([^"]+)"\s*\)',code):
            action=OPERATIONAL_LEGACY_CAPTIONS.get(caption)
            if action is None:continue
            ident=owner+"#"+variable
            if ident not in controls:
                raise ValueError("Review missing Java control "+ident)
            out["specific."+ident]=OPERATIONAL_COPY[locale][action]
    for short_name,action in RESTAURANT_ACTION_TARGETS.items():
        ident="com.mx.kylgis.pos."+short_name
        if ident not in controls:
            raise ValueError("Missing reviewed restaurant control "+ident)
        out["specific."+ident]=RESTAURANT_ACTION_COPY[locale][action]
    # Manually verified exceptions have highest priority. JParamsUser's
    # customer-looking icon actually opens the employee selection dialog.
    for short_name,action in OPERATIONAL_TARGETS.items():
        ident="com.mx.kylgis.pos."+short_name
        if ident not in controls:
            raise ValueError("Missing mapped control "+ident)
        out["specific."+ident]=OPERATIONAL_COPY[locale][action]
    for name,(typ,semantic) in controls.items():
        # Native sentences in the locale: field captions are taken from Swing
        # itself; control catalog provides stable fallback for icon-only items.
        domain=safe_text(" ".join(out.get("term."+token,token) for token in semantic.split()))
        business=BUSINESS_FIELD_CONTEXTS.get(name.split("#",1)[0])
        if business and " ("+business+")" in semantic:
            kind_of_field=semantic.split(" (",1)[0]
            core=BUSINESS_DOMAIN_TRANSLATIONS[locale][business]
            if kind_of_field=="name":
                domain=out["term.name"]+" ("+core+")"
            elif kind_of_field=="label":
                domain=core
            else:
                domain=core
        category={
            "JButton":"button","JMenuItem":"button","JToggleButton":"button",
            "JLabel":"button",
            "JTextField":"input","JTextArea":"input","JFormattedTextField":"input",
            "JPasswordField":"password","JComboBox":"select",
            "JCheckBox":"check","JRadioButton":"radio",
            "JTable":"table","JList":"list","JTabbedPane":"tab","JSpinner":"range"
        }.get(typ,"input")
        # Specific descriptions allow developers to alter one control without
        # changing global UX templates.
        sentence=strings[FORMATS.index(category)].replace("{0}",domain)
        hint=legacy_hints.get(name)
        if hint is not None:
            action,legacy_key=hint
            translation=native_resource_message(locale,legacy_key)
            if action=="setToolTipText":
                # Empty is intentional. It masks the base-language native
                # copy when this locale has no translation, so the localized
                # autogenerated sentence is used instead of English.
                out["native."+name]=translation or ""
            if translation:
                if action=="setToolTipText":
                    sentence=translation
                    # Expert-written tooltip already localized by legacy POS:
                    # prefer the copy sealed inside the JAR over a generic
                    # caption-derived tooltip, even when a button has text.
                elif "btn" in name.lower() or typ in ("JButton","JToggleButton","JMenuItem","JLabel"):
                    sentence=strings[0].replace("{0}",translation)
                else:
                    sentence=strings[FORMATS.index(category)].replace("{0}",translation)
        out["control."+name]=sentence
    header=[
        "# Embedded, UTF-8, compiled into kylgispos.jar. Never loaded from user locales.",
        "# Generated from actual Swing fields by tools/generate_embedded_ui_help.py.",
        "# Edit SPECIAL/PHRASES and run --write; run --check in release QA.",
        f"# locale={locale}, controls={len(controls)}, captions={len(literals)}"
    ]
    content="\n".join(header)+"\n" + "\n".join(
        k+"="+escape(str(v)) for k,v in sorted(out.items()))+"\n"
    return content

def generate():
    controls=collect_controls()
    literals=collect_literals()
    meta="\n".join(["# KylGis embedded Swing control registry. Changes must be committed."]+
            [f"{k}={kind}|{words}" for k,(kind,words) in sorted(controls.items())])+"\n"
    resources={"ui_help_catalog.properties":meta}
    expected={"base","al_SQ","ar","da","de","el","en_US","es","es_AR","es_MX","et","fr","hr","it","nl","pt","pt_BR"}
    assert set(PHRASES)==expected
    for locale in sorted(PHRASES):
        filename="ui_help.properties" if locale=="base" else f"ui_help_{locale}.properties"
        resources[filename]=write_one(locale,controls,literals)
    return resources,len(controls),len(literals)

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--write",action="store_true")
    parser.add_argument("--check",action="store_true")
    args=parser.parse_args()
    resources,n,labels=generate()
    if args.write:
        for filename,content in resources.items():
            (RES/filename).write_text(content,encoding="utf-8")
    errors=[]
    for filename,content in resources.items():
        path=RES/filename
        if not path.exists() or path.read_text(encoding="utf-8")!=content:
            errors.append(filename)
    print(f"UI_HELP_BUNDLES={len(resources)-1} CONTROLS={n} STATIC_LITERAL_KEYS={labels} DRIFT={len(errors)}")
    if errors:
        print("MISMATCH",",".join(errors))
        if args.check:raise SystemExit(2)
    if args.check:
        print("UI_HELP_EMBEDDED_RESOURCE_PARITY_PASS")
if __name__=="__main__":main()
