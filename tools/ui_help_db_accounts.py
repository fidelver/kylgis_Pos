"""Localized and distinct database credential help.

Never put descriptive text in the username value. Primary and secondary
accounts share translations but not their actual configuration values.
"""
LANG={
"base":("Enter the database account username (not the password).",
        "Enter the password for this database account; the value is hidden."),
"al_SQ":("Shkruani emrin e përdoruesit të bazës së të dhënave, jo fjalëkalimin.",
         "Shkruani fjalëkalimin e llogarisë së bazës; ai fshihet."),
"ar":("أدخل اسم مستخدم حساب قاعدة البيانات، وليس كلمة المرور.",
       "أدخل كلمة مرور حساب قاعدة البيانات؛ تبقى مخفية."),
"da":("Indtast databasekontoens brugernavn, ikke adgangskoden.",
        "Indtast databasekontoens adgangskode; den er skjult."),
"de":("Geben Sie den Benutzernamen des Datenbankkontos ein, nicht das Kennwort.",
        "Geben Sie das Kennwort des Datenbankkontos ein; es wird verborgen."),
"el":("Εισαγάγετε το όνομα χρήστη της βάσης δεδομένων, όχι τον κωδικό πρόσβασης.",
        "Εισαγάγετε τον κωδικό πρόσβασης του λογαριασμού βάσης· αποκρύπτεται."),
"es":("Capture el nombre de usuario de la base de datos, no su contraseña.",
       "Capture la contraseña de la cuenta de base de datos; permanece oculta."),
"et":("Sisesta andmebaasikonto kasutajanimi, mitte parool.",
        "Sisesta andmebaasikonto parool; see on peidetud."),
"fr":("Saisissez le nom d'utilisateur du compte de base de données, pas son mot de passe.",
        "Saisissez le mot de passe du compte de base de données ; il reste masqué."),
"hr":("Unesite korisničko ime baze podataka, a ne lozinku.",
        "Unesite lozinku računa baze podataka; ona ostaje skrivena."),
"it":("Inserisci il nome utente dell'account database, non la password.",
        "Inserisci la password del database; il valore rimane nascosto."),
"nl":("Voer de gebruikersnaam van de databaseaccount in, niet het wachtwoord.",
        "Voer het wachtwoord van de databaseaccount in; het wordt verborgen."),
"pt":("Introduza o nome de utilizador da conta da base de dados, não a senha.",
        "Introduza a palavra-passe da conta da base de dados; fica oculta."),
"pt_BR":("Digite o usuário do banco de dados, não a senha.",
        "Digite a senha da conta do banco de dados; ela fica oculta."),
}
LANG["en_US"]=LANG["base"]
LANG["es_AR"]=LANG["es"]
LANG["es_MX"]=LANG["es"]
assert len(LANG)==17
TRANSLATIONS={
 loc:{"tooltip.database.account.user":user,
      "tooltip.database.account.password":password}
 for loc,(user,password) in LANG.items()}

MULTIDB={
"base":"Enable a secondary database connection; disable it to use only the main database.",
"al_SQ":"Aktivizoni lidhjen me bazën dytësore; çaktivizojeni për të përdorur vetëm bazën kryesore.",
"ar":"فعّل الاتصال بقاعدة بيانات ثانوية؛ عطّله لاستخدام قاعدة البيانات الرئيسية فقط.",
"da":"Aktivér en sekundær databaseforbindelse; deaktivér for kun at bruge hoveddatabasen.",
"de":"Aktiviert eine zweite Datenbankverbindung; deaktivieren für nur die Hauptdatenbank.",
"el":"Ενεργοποιεί σύνδεση δεύτερης βάσης· απενεργοποιήστε για χρήση μόνο της κύριας.",
"es":"Activa una conexión de base de datos secundaria; desactívela para usar únicamente la principal.",
"et":"Lülita teisese andmebaasi ühendus sisse; välja lülitades kasutatakse ainult põhiteenust.",
"fr":"Active une connexion à une base secondaire ; désactivez pour utiliser seulement la base principale.",
"hr":"Omogući vezu sa sekundarnom bazom; isključi za korištenje samo glavne baze.",
"it":"Attiva una connessione al database secondario; disattivala per usare solo quello principale.",
"nl":"Schakel een tweede databaseverbinding in; schakel uit om alleen de primaire database te gebruiken.",
"pt":"Ativa uma ligação à base de dados secundária; desative para usar só a principal.",
"pt_BR":"Ativa a conexão com o banco de dados secundário; desative para usar somente o principal.",
}
MULTIDB["en_US"]=MULTIDB["base"]
MULTIDB["es_AR"]=MULTIDB["es"]
MULTIDB["es_MX"]=MULTIDB["es"]
assert set(MULTIDB)==set(TRANSLATIONS)
for locale,tip in MULTIDB.items():
    TRANSLATIONS[locale]["tooltip.database.account.multidb"]=tip
