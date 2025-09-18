
STEP 2
- Migliorare la vista dell'Account UI
- Migliorare la vista remote control
- Gestione del secondo colore di maglietta
- Gestione di bannter multipli temporizzati (se possibile animati)
- Implementare Video full screen per spot/pubblicità
- Premettere di configurarsi il proprio firebase per remote
- Se autenticato aprire su Youtube
- Unificare la CameraFragmente e la UVCCameraFragment per le parti comuni.
- Valutare anche il goLive() soprattutto UI!!!
- 
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

Generate Key from account name oe mail, and some random part ?? use AES encripton??
License Key:


https://avisbiella.it/wp-content/uploads/2022/01/Logo_AVIS.png
https://i.pinimg.com/originals/d8/6d/81/d86d816c89cc355af1bd2ef96ca96d62.png
https://cdn.verovolley.com/wp-content/uploads/elementor/thumbs/cropped-logo-vero-volley-png-quadrato-1-qgzs9a903nsl3fis33qoyl3bwl7z2ugng6i5acp60o.png
https://live2sport.com/image/Volleyball_W_Italy_Picco_Lecco.png
https://www.nowpadova.com/images/2020/01/23/AVIST_large.jpg



/*navController.addOnDestinationChangedListener { controller, destination, arguments ->
    try {
        val rootId = navController.graph.id
        val backStackEntries = mutableListOf<String>()

        var currentEntry: NavBackStackEntry? = navController.currentBackStackEntry
        while (currentEntry != null) {
            backStackEntries.add(currentEntry.destination.label.toString())
            if (currentEntry.destination.id == rootId) break
            currentEntry = try {
                navController.getBackStackEntry(currentEntry.destination.id - 1)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        Log.d("LM_NAV_DEBUG", "Back stack: ${backStackEntries.joinToString(" -> ")}")
    } catch (e: Exception) {
        Log.e("LM_NAV_DEBUG", "Error reading back stack", e)
    }
}*/