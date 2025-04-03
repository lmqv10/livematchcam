versione free
+ rtmp / key
+ auto zoom
+ nomi
+ colore maglia
+ score


next step:
+ loghi
+ sponsor banners  
+ spot video
+ stats

-- remote commands "nearby"
-- remote configurations
-- replay (slow motion)

https://i-predict.it/assets/loghi-hd/Venezia.png
https://i-predict.it/assets/loghi-hd/Roma.png
https://avisbiella.it/wp-content/uploads/2022/01/Logo_AVIS.png
https://i.pinimg.com/originals/d8/6d/81/d86d816c89cc355af1bd2ef96ca96d62.png
https://logodix.com/logo/1746008.png
https://cdn.verovolley.com/wp-content/uploads/elementor/thumbs/cropped-logo-vero-volley-png-quadrato-1-qgzs9a903nsl3fis33qoyl3bwl7z2ugng6i5acp60o.png
https://live2sport.com/image/Volleyball_W_Italy_Picco_Lecco.png
https://www.nowpadova.com/images/2020/01/23/AVIST_large.jpg

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

Login.. with 
Start up
>> ask for last five match.. youtube api
>> ask for firebase matches (Configured) 
  - Clean old matches
  - Add new matches with default value (no logo, team name)
  - Store new data
>> all data will be sync beetween account
--> Edit data in app (sync with firebase database and all connected devices

License Key:



val imageView = ImageView(requireContext())
                    imageView.load(spotBannerURL) {
                        allowHardware(false)
                        listener(
                            onError = { _, error ->
                                Logd(error.throwable.message ?: "ERROR")
                            },
                            onSuccess = { _, result ->
                                try {
                                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                                    val maxFactor = 20f
                                    val defaultScaleX = ((bitmap?.width?.times(100) ?: 0) / width).toFloat()
                                    val defaultScaleY = ((bitmap?.height?.times(100) ?: 0) / height).toFloat()

                                    Logd("spotBannerURL: width : ${bitmap?.width} x height : ${bitmap?.height}")

                                    val factorX = maxFactor / defaultScaleX
                                    val scaleX = factorX * defaultScaleX
                                    val scaleY = factorX * defaultScaleY

                                    Logd("spotBannerURL: scaleX : $scaleX - scaleY : $scaleY")

                                    var spotBannerFilter = ImageObjectFilterRender()
                                    spotBannerFilter.apply {
                                        setImage(bitmap)
                                        setScale(scaleX, scaleY)
                                        setAlpha(0.75f)
                                        setPosition(100f - scaleX, 0f)
                                    }
                                    genericStream.getGlInterface().setFilter(1, spotBannerFilter)
                                } catch (e : Exception) {
                                    Logd("spotBannerURL: scaleX : ${e.message}")
                                }
                            }
                        )
                    }
