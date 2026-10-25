"""Semantic messages for applying peripherals, mirror navigation and config errors."""
from ui_help_static_translations import TRANSLATIONS as LITERALS
# Tooltip explanations: apply/sync hardware without reboot; scroll to mirror options;
# save failure warning. Nothing here contains credentials or user-supplied text.
WORDS={
"base":(
"Save and reload printer, customer display, scale, scanner and iButton settings without restarting the POS.",
"Scroll to the screen and digital receipt mirror output settings.",
"The configuration could not be saved."),
"al_SQ":(
"Ruani dhe ringarkoni cilësimet e printerëve, ekranit, peshores, skanerit dhe iButton pa rifilluar POS-in.",
"Kaloni te cilësimet e kopjeve të kuponit në ekran dhe në format digjital.",
"Konfigurimi nuk mund të ruhej."),
"ar":(
"احفظ وأعد تحميل إعدادات الطابعات وشاشة العميل والميزان والماسح وiButton دون إعادة تشغيل نظام البيع.",
"انتقل إلى إعدادات النسخة المعروضة على الشاشة والإيصال الرقمي.",
"تعذر حفظ الإعدادات."),
"da":(
"Gem og genindlæs printer-, kundevisnings-, vægt-, scanner- og iButton-indstillinger uden at genstarte POS.",
"Rul til indstillingerne for skærmkopi og digital kvittering.",
"Konfigurationen kunne ikke gemmes."),
"de":(
"Speichert und lädt Drucker-, Kundendisplay-, Waagen-, Scanner- und iButton-Einstellungen neu, ohne das POS neu zu starten.",
"Scrollt zu den Einstellungen für Bildschirmkopie und digitalen Bon.",
"Die Konfiguration konnte nicht gespeichert werden."),
"el":(
"Αποθηκεύει και επαναφορτώνει εκτυπωτές, οθόνη πελάτη, ζυγαριά, σαρωτή και iButton χωρίς επανεκκίνηση του POS.",
"Μεταβαίνει στις ρυθμίσεις αντιγράφων στην οθόνη και ψηφιακής απόδειξης.",
"Δεν ήταν δυνατή η αποθήκευση των ρυθμίσεων."),
"es":(
"Guarda y recarga impresoras, visor de cliente, báscula, escáner e iButton sin reiniciar el POS.",
"Desplaza la pantalla hasta la configuración de copias de recibos en pantalla y ticket digital.",
"No se pudo guardar la configuración."),
"et":(
"Salvesta ja laadi printeri, kliendiekraani, kaalu, skanneri ning iButtoni seaded ilma POS-i taaskäivitamata.",
"Keri ekraanikoopia ja digitaalse kviitungi säteteni.",
"Seadistusi ei õnnestunud salvestada."),
"fr":(
"Enregistre et recharge les imprimantes, l’afficheur client, la balance, le scanner et iButton sans redémarrer le POS.",
"Fait défiler jusqu’aux paramètres de copie écran et de reçu numérique.",
"Impossible d’enregistrer la configuration."),
"hr":(
"Spremi i ponovno učitaj pisače, zaslon kupca, vagu, skener i iButton bez ponovnog pokretanja POS-a.",
"Pomakni se do postavki kopije računa na zaslonu i digitalnog računa.",
"Postavke nije moguće spremiti."),
"it":(
"Salva e ricarica le impostazioni di stampanti, display cliente, bilancia, scanner e iButton senza riavviare il POS.",
"Scorri alle impostazioni della copia su schermo e della ricevuta digitale.",
"Impossibile salvare la configurazione."),
"nl":(
"Sla printer-, klantendisplay-, weegschaal-, scanner- en iButton-instellingen op en laad ze opnieuw zonder de POS te herstarten.",
"Scrol naar de instellingen voor schermkopie en digitale bon.",
"De configuratie kon niet worden opgeslagen."),
"pt":(
"Guarda e recarrega as definições de impressoras, visor do cliente, balança, scanner e iButton sem reiniciar o POS.",
"Desloca até às definições da cópia no ecrã e do recibo digital.",
"Não foi possível guardar a configuração."),
"pt_BR":(
"Salva e recarrega impressoras, visor do cliente, balança, scanner e iButton sem reiniciar o PDV.",
"Rola até as configurações da cópia na tela e do comprovante digital.",
"Não foi possível salvar as configurações.")
}
WORDS["en_US"]=WORDS["base"]
WORDS["es_AR"]=WORDS["es"]
WORDS["es_MX"]=WORDS["es"]
assert len(WORDS)==17
assert all(len(v)==3 for v in WORDS.values())
MESSAGES={}
for locale,(apply_help,mirror_help,error) in WORDS.items():
    MESSAGES[locale]={
        "text.peripheral.apply":LITERALS["Aplicar periféricos"][locale],
        "tooltip.peripheral.apply":apply_help,
        "tooltip.peripheral.mirror.scroll":mirror_help,
        "text.config.error.save":error,
    }
