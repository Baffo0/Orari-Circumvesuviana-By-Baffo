# Orari Circumvesuviana By Baffo

Tutte le fermate della Circumvesuviana sulle linee **Napoli – Sorrento** e **Napoli – Poggiomarino**, ognuna collegata al tabellone partenze in tempo reale di EAV (treno, destinazione, binario, orario e ritardo).

Sito: https://baffo0.github.io/Orari-Circumvesuviana-By-Baffo/

I tabelloni sono quelli ufficiali EAV: `https://orariotreni.eavsrl.it/teleindicatori/?stazione=<numero>`.
I numeri delle stazioni sono negli elenchi `TRUNK` e `LINES` dentro `index.html`.

## App Android (VesuOra)

La cartella `android/` contiene l'app: una schermata con il sito incluso nell'APK. I tabelloni EAV si aprono dentro l'app (Indietro torna all'elenco, trascina giù per aggiornare).

A ogni modifica di `index.html` o `android/`, GitHub Actions compila l'APK e lo pubblica sempre qui:

- https://github.com/Baffo0/Orari-Circumvesuviana-By-Baffo/releases/download/apk/vesuora.apk
- se il download resta fermo al 100%: https://github.com/Baffo0/Orari-Circumvesuviana-By-Baffo/releases/download/apk/vesuora.zip
