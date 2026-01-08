
BUGS
- Risoluzionei camera sorgente dinamiche
- Freeze Preview during streaming,. possibile cambio resolution durante lo streaming?
- Team name più lunghi per la pallavolo. 3 per il calcio.
- Verificare caratteri e risoluzione schermo device.
- Banners rotation con lista e configurabile
- Streams Configurabili

NOTA:: RIVEDERE TUTTE LE DIALOG PER init e non OnCReate
.. Es. CameraSettingsDialog.. se funziona... 

STEP 2

- Gestione ACCOUNT/MENU/FRAGMENT ...
- Migliorare la vista dell'Account UI
- Migliorare la vista remote control
- Gestione del secondo colore di maglietta
- Gestione di bannter multipli temporizzati (se possibile animati)
- Implementare Video full screen per spot/pubblicità
- Premettere di configurarsi il proprio firebase per remote
- Valutare anche il goLive() soprattutto UI!!!
- Gestione flag YouTubeEnabled (on Firebase or...)

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
https://www.nowpadova.com/images/2020/01/23/AVIST_large.jpg

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

/*
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val ids = cameraManager.cameraIdList
    for (cameraId in ids) {
        val cameraFacing = cameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_FACING)
        if (cameraFacing != null && cameraFacing == CameraMetadata.LENS_FACING_BACK) {
        var characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val maxZoom = characteristics.secureGet(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1f
    }
}
*/