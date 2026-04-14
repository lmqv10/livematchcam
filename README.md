========
App
========
Preview Banner in Controllo remoto.
Login Youtube attivo SOLO per admin.


========
Backend
========
Matches in Backend con nome squadra (team vs Stream)
Ordinamento matches.
Metrica della connessione delle partite live (su app) , segnala al backend. "nodo Metrics".
Golive da backend
Lettura API youtube per stato live matches (e, link di riferimento)
Timestamp di ultima modifica del punteggio
Monitoring di stato live e punteggio da "teams"
Ordinamento dinamico (last modified desc)
Loghi con repository e storage in Firestore
Programmazionc con Bind Scollegato da "pianificazione".
Import Visuale (con pannello di modifica)
Polisportiva (utenti con diverso ruolo amministrativo / sportivo) per gestione partite.


WIP 

NEXT CANDIDATE
- Banners Video full screen per spot/pubblicità
- Banners temporizzabili (25 sec)
- Notifica al dispositivo in app per contattare streamer. (popup in app).
- Battery Level warning (preference "show/hide battery level"). NOTA: il monitoring potrebbe costare.

TODO LIST
- Banners rotation con lista e configurabile se possibile animati
- News Bar bottom
- Gestione tempi calcio più completa
- Gestione Loghi con "nome" / git repository?
- Gestione Ruoli (administrator/operator)
- Autenticazione YOUTUBE (owner/ branding channel).
- Gestione del secondo colore di maglietta
- Verificare caratteri e risoluzione schermo device.
- Risoluzioni camera sorgente dinamiche
- Visibilità delle preferences su "availability del device"

PRO
- Integrazione Social (post screenshot punteggio)
- Speed Test e wizard di configurazione streaming.
- Gestione delle autenticazioni, della sicurezza, dei dispositivi.
  consentire a dispositivi di trasmettere soltanto con alcuni account
- aggiungere mfa,
- mettere in Black list dispositivi,
- disattivare o inibire audio da remoto and so on.

MUST HAVE
- Valutare anche il goLive() soprattutto UI!!!
  Richiede API youtube - richiede account autenticato su youtube
  TBE:
    - Preferences Request "GoLive" / "End Stream"
    - avviare lo streaming su control room (con conferma).
    - se attivo il bottone diventa un "play" e mette un testo "contenuto non pubblico" ben visibile.
    - il bottone "play" "invia" il "GoLive" (con conferma) e far sparire il testo "contenuto non pubblico"
    - il bottone "play" diventa "stop" (quadrato)
    - lo "stop" chiede anche l'"end stream"

LOWER PRIORITY
- Censimento Palestre e Qualità della rete. Censimento e DB consultabile
- Migliorare la vista dell'Account UI
- Migliorare la vista remote control
- Gestione flag YouTubeEnabled (on Firebase or...)
- Permettere di configurarsi il proprio firebase per remote

NOTA:: RIVEDERE TUTTE LE DIALOG PER init e non OnCReate
.. Es. CameraSettingsDialog.. se funziona...

To be verify
-- remote commands "nearby"
-- remote configurations
-- replay (slow motion)

Obiettivo:
Creare un'applicazione per organizzare,
gestire e trasmettere su YouTube
(e altri canali)
tutte le partite del settore giovanile.

Semplicità d'Uso:
Chi riprende non deve inserire impostazioni o chiavi, ma solo avviare il live e aggiornare il punteggio.
Pianificazione Remota:
Partite, nomi e loghi delle squadre pianificati in anticipo da remoto e centralizzati.
Punteggio in Tempo Reale e comunicazione agli interessati:
Punteggio salvato e disponibile in tempo reale per ogni partita in corso e visualizzabile sull’app.
Comunicazione a una lista di utenti (Whatsapp) dell’avvio della diretta
Funzionalità Per gli sponsor:
Pulsante 'SPOT' per trasmettere spot pubblicitari durante il cambio set.
Pulsante 'TimeOut' per trasmettere un'immagine pubblicitaria con filigrana di 20 secondi.

Licenze per Società:
Le licenze sono declinate per società, non per singolo utente.
Periodo di Validità:
Le licenze coincidono con la stagione sportiva o frazioni di stagione.
Licenze Concomitanti:
Calcolare i soli utenti che utilizzano contemporaneamente l’APP.
Utenti Autorizzati:
Definizione di utenti autorizzati ad utilizzare il servizio.

Generate Key from account name o email, and some random part ?? use MD5 AES encripton??
License Key:
Masseroni
3a1fcfd8b3664b72b5b745760819bd8c
Lecco
846af82686b3429a85e9a2d9a14ed79a
Picco
99261ce7d8b94fc395301f57d9e61ffd
-> MD5: 4c5fa624caf169096501319def233cc9
Arosio
f67fee1fd18f3ce57855964b806a4f3a
-> MD5: 380d9d52ddb191dd75761827ec6a784e

https://avisbiella.it/wp-content/uploads/2022/01/Logo_AVIS.png
https://www.nowpadova.com/images/2020/01/23/AVIST_large.jpg


https://raw.githubusercontent.com/paolodito-lab/loghi/refs/heads/main/ACCIATUBI_PICCO_VIDEO.png
