"""Native report command and date-range control explanations in all locales."""
KEYS=("toggleFilters","execute","startCalendar","endCalendar")
DATA={
"base":(
"Show or hide the report filters without changing the selected values.",
"Run the report using the selected filters and display the results.",
"Open the calendar to choose the start date of the report interval.",
"Open the calendar to choose the end date of the report interval."),
"al_SQ":(
"Shfaqni ose fshihni filtrat e raportit pa ndryshuar vlerat e zgjedhura.",
"Ekzekutoni raportin me filtrat e zgjedhur dhe shfaqni rezultatet.",
"Hapni kalendarin për të zgjedhur datën e fillimit të raportit.",
"Hapni kalendarin për të zgjedhur datën e përfundimit të raportit."),
"ar":(
"اعرض عوامل تصفية التقرير أو أخفها دون تغيير القيم المحددة.",
"شغّل التقرير باستخدام عوامل التصفية المحددة واعرض النتائج.",
"افتح التقويم لاختيار تاريخ بداية فترة التقرير.",
"افتح التقويم لاختيار تاريخ نهاية فترة التقرير."),
"da":(
"Vis eller skjul rapportfiltrene uden at ændre de valgte værdier.",
"Kør rapporten med de valgte filtre, og vis resultaterne.",
"Åbn kalenderen for at vælge rapportperiodens startdato.",
"Åbn kalenderen for at vælge rapportperiodens slutdato."),
"de":(
"Zeigt oder verbirgt die Berichtsfilter, ohne die ausgewählten Werte zu ändern.",
"Führt den Bericht mit den ausgewählten Filtern aus und zeigt die Ergebnisse.",
"Öffnet den Kalender zur Auswahl des Beginns des Berichtszeitraums.",
"Öffnet den Kalender zur Auswahl des Endes des Berichtszeitraums."),
"el":(
"Εμφανίζει ή αποκρύπτει τα φίλτρα της αναφοράς χωρίς να αλλάζει τις τιμές.",
"Εκτελεί την αναφορά με τα επιλεγμένα φίλτρα και εμφανίζει τα αποτελέσματα.",
"Ανοίγει το ημερολόγιο για επιλογή της ημερομηνίας έναρξης.",
"Ανοίγει το ημερολόγιο για επιλογή της ημερομηνίας λήξης."),
"es":(
"Muestra u oculta los filtros del reporte sin modificar los valores seleccionados.",
"Ejecuta el reporte con los filtros elegidos y muestra los resultados.",
"Abre el calendario para elegir la fecha inicial del periodo del reporte.",
"Abre el calendario para elegir la fecha final del periodo del reporte."),
"et":(
"Näita või peida aruande filtrid ilma valitud väärtusi muutmata.",
"Käivita aruanne valitud filtritega ja kuva tulemused.",
"Ava kalender aruande perioodi alguskuupäeva valimiseks.",
"Ava kalender aruande perioodi lõpukuupäeva valimiseks."),
"fr":(
"Affiche ou masque les filtres du rapport sans modifier leurs valeurs.",
"Exécute le rapport avec les filtres choisis et affiche les résultats.",
"Ouvre le calendrier pour choisir la date de début du rapport.",
"Ouvre le calendrier pour choisir la date de fin du rapport."),
"hr":(
"Prikaži ili sakrij filtre izvještaja bez promjene odabranih vrijednosti.",
"Pokreni izvještaj s odabranim filtrima i prikaži rezultate.",
"Otvori kalendar za odabir početnog datuma izvještaja.",
"Otvori kalendar za odabir završnog datuma izvještaja."),
"it":(
"Mostra o nasconde i filtri del report senza modificare i valori selezionati.",
"Esegue il report con i filtri scelti e mostra i risultati.",
"Apre il calendario per scegliere la data iniziale del report.",
"Apre il calendario per scegliere la data finale del report."),
"nl":(
"Toon of verberg rapportfilters zonder de geselecteerde waarden te wijzigen.",
"Voer het rapport uit met de gekozen filters en toon de resultaten.",
"Open de kalender om de begindatum van het rapport te kiezen.",
"Open de kalender om de einddatum van het rapport te kiezen."),
"pt":(
"Mostra ou oculta os filtros do relatório sem alterar os valores escolhidos.",
"Executa o relatório com os filtros selecionados e mostra os resultados.",
"Abre o calendário para escolher a data inicial do relatório.",
"Abre o calendário para escolher a data final do relatório."),
"pt_BR":(
"Mostra ou oculta os filtros do relatório sem alterar os valores escolhidos.",
"Executa o relatório com os filtros selecionados e mostra os resultados.",
"Abre o calendário para escolher a data inicial do relatório.",
"Abre o calendário para escolher a data final do relatório.")
}
DATA["en_US"]=DATA["base"]
DATA["es_AR"]=DATA["es"]
DATA["es_MX"]=DATA["es"]
assert len(DATA)==17
assert all(len(v)==len(KEYS) for v in DATA.values())
TRANSLATIONS={loc:{"tooltip.report."+key:text for key,text in zip(KEYS,values)}
              for loc,values in DATA.items()}
