"""Contextual HTML labels with native POS locale translations.

Full messages are localized before Swing renders HTML; Java never concatenates
partially translated markup or interpolates user-supplied values here.
"""
KEYS=("peripheral.mirror.warning","peripheral.mirror.more",
      "peripheral.ticket.note","branding.title")
LANG={
"base":(
"<html><b>Warning:</b> configure the primary receipt printer as Printer 1. The Screen and Digital Receipt mirror outputs are linked to Printer 1.</html>",
"<html><b>More options:</b> receipt screen and digital receipt.</html>",
"<html><b>Note:</b> Screen and Digital Receipt mirror the primary printer. We recommend Printer 1 for receipts.</html>",
"KylGis POS Touch Point of Sale"),
"al_SQ":(
"<html><b>Paralajmërim:</b> konfiguroni printerin kryesor të kuponit si Printer 1. Kopjet në ekran dhe kuponi digjital lidhen me Printer 1.</html>",
"<html><b>Më shumë opsione:</b> ekrani i kuponit dhe kuponi digjital.</html>",
"<html><b>Shënim:</b> Ekrani dhe kuponi digjital pasqyrojnë printerin kryesor. Rekomandohet Printer 1 për kuponët.</html>",
"KylGis POS Pikë shitjeje me prekje"),
"ar":(
"<html><b>تحذير:</b> اضبط طابعة الإيصالات الرئيسية على Printer 1. ترتبط نسخ الشاشة والإيصال الرقمي بالطابعة Printer 1.</html>",
"<html><b>خيارات إضافية:</b> شاشة الإيصال والإيصال الرقمي.</html>",
"<html><b>ملاحظة:</b> الشاشة والإيصال الرقمي نسختان من الطابعة الرئيسية. نوصي باستخدام Printer 1 للإيصالات.</html>",
"KylGis POS نقطة بيع باللمس"),
"da":(
"<html><b>Advarsel:</b> indstil den primære bonprinter til Printer 1. Skærmkopi og digital bon er knyttet til Printer 1.</html>",
"<html><b>Flere muligheder:</b> bonskærm og digital bon.</html>",
"<html><b>Bemærk:</b> Skærm og digital bon spejler hovedprinteren. Printer 1 anbefales til bonner.</html>",
"KylGis POS Berøringsbaseret kassesystem"),
"de":(
"<html><b>Warnung:</b> Legen Sie Printer 1 als Hauptbondrucker fest. Bildschirmkopie und digitaler Bon sind an Printer 1 gebunden.</html>",
"<html><b>Weitere Optionen:</b> Bonanzeige und digitaler Bon.</html>",
"<html><b>Hinweis:</b> Bildschirm und digitaler Bon spiegeln den Hauptdrucker. Für Bons wird Printer 1 empfohlen.</html>",
"KylGis POS Touch-Kassensystem"),
"el":(
"<html><b>Προειδοποίηση:</b> ορίστε το Printer 1 ως κύριο εκτυπωτή αποδείξεων. Η οθόνη και η ψηφιακή απόδειξη συνδέονται με το Printer 1.</html>",
"<html><b>Περισσότερες επιλογές:</b> οθόνη απόδειξης και ψηφιακή απόδειξη.</html>",
"<html><b>Σημείωση:</b> Η οθόνη και η ψηφιακή απόδειξη αντιγράφουν τον κύριο εκτυπωτή. Συνιστάται το Printer 1.</html>",
"KylGis POS Σύστημα αφής σημείου πώλησης"),
"es":(
"<html><b>Advertencia:</b> configure la impresora principal de recibos como Printer 1. Las copias en pantalla y el ticket digital están vinculados a Printer 1.</html>",
"<html><b>Más opciones:</b> Pantalla de recibo y Ticket digital.</html>",
"<html><b>Nota:</b> Pantalla y Ticket digital son copias de la impresora principal. Se recomienda usar Printer 1 para los recibos.</html>",
"KylGis POS Punto de Venta Táctil"),
"et":(
"<html><b>Hoiatus:</b> seadistage põhikviitungiprinteriks Printer 1. Ekraanikoopia ja digitaalne kviitung on seotud Printer 1-ga.</html>",
"<html><b>Rohkem valikuid:</b> kviitungiekraan ja digitaalne kviitung.</html>",
"<html><b>Märkus:</b> Ekraan ja digitaalne kviitung peegeldavad põhiprinterit. Kviitungite jaoks soovitame Printer 1.</html>",
"KylGis POS Puutetundlik kassasüsteem"),
"fr":(
"<html><b>Avertissement :</b> configurez Printer 1 comme imprimante principale des reçus. Les copies à l’écran et le reçu numérique sont liés à Printer 1.</html>",
"<html><b>Plus d’options :</b> écran du reçu et reçu numérique.</html>",
"<html><b>Remarque :</b> L’écran et le reçu numérique reproduisent l’imprimante principale. Printer 1 est recommandé pour les reçus.</html>",
"KylGis POS Caisse tactile"),
"hr":(
"<html><b>Upozorenje:</b> postavite Printer 1 kao glavni pisač računa. Zaslonska kopija i digitalni račun povezani su s Printer 1.</html>",
"<html><b>Više mogućnosti:</b> zaslon računa i digitalni račun.</html>",
"<html><b>Napomena:</b> Zaslon i digitalni račun zrcale glavni pisač. Preporučuje se Printer 1.</html>",
"KylGis POS Prodajno mjesto s dodirom"),
"it":(
"<html><b>Attenzione:</b> imposta Printer 1 come stampante principale delle ricevute. Copia su schermo e ricevuta digitale sono collegate a Printer 1.</html>",
"<html><b>Altre opzioni:</b> schermo della ricevuta e ricevuta digitale.</html>",
"<html><b>Nota:</b> Schermo e ricevuta digitale rispecchiano la stampante principale. Si consiglia Printer 1.</html>",
"KylGis POS Punto vendita touch"),
"nl":(
"<html><b>Waarschuwing:</b> stel Printer 1 in als primaire bonprinter. Schermkopie en digitale bon zijn gekoppeld aan Printer 1.</html>",
"<html><b>Meer opties:</b> bon op scherm en digitale bon.</html>",
"<html><b>Opmerking:</b> Het scherm en de digitale bon spiegelen de hoofdprinter. Printer 1 wordt aanbevolen.</html>",
"KylGis POS Touch-kassasysteem"),
"pt":(
"<html><b>Aviso:</b> configure Printer 1 como impressora principal dos recibos. A cópia no ecrã e o recibo digital estão associados a Printer 1.</html>",
"<html><b>Mais opções:</b> ecrã do recibo e recibo digital.</html>",
"<html><b>Nota:</b> O ecrã e o recibo digital reproduzem a impressora principal. Recomenda-se Printer 1.</html>",
"KylGis POS Ponto de venda tátil"),
"pt_BR":(
"<html><b>Aviso:</b> configure Printer 1 como impressora principal dos comprovantes. A cópia na tela e o comprovante digital estão vinculados a Printer 1.</html>",
"<html><b>Mais opções:</b> tela do comprovante e comprovante digital.</html>",
"<html><b>Nota:</b> Tela e comprovante digital espelham a impressora principal. Recomendamos Printer 1.</html>",
"KylGis POS Ponto de venda por toque")
}
LANG["en_US"]=LANG["base"]
LANG["es_AR"]=LANG["es"]
LANG["es_MX"]=LANG["es"]
assert len(LANG)==17
assert all(len(v)==len(KEYS) for v in LANG.values())
TRANSLATIONS={locale:{"text."+key:values[i] for i,key in enumerate(KEYS)}
              for locale,values in LANG.items()}
