# pakrunner

*Pakrunner* je RESTful servis za kontrolu i monitoring dugotrajnih proračuna. 

## Primeri poziva

### Pokretanje proračuna
``curl -d '{"guid":"3333-4444", "command":"./proba.sh"}' -H "Content-Type: application/json" -X POST http://147.91.200.5:8081/pakrunner/rest/api/start``

### Da li proračun radi?
``curl -H "Content-Type: application/json" -X GET http://147.91.200.5:8081/pakrunner/rest/api/isrunning/3333-4444``

### Zaustavljanje proračuna
`curl -d '{"guid":"3333-4444"}' -H "Content-Type: application/json" -X POST http://147.91.200.5:8081/pakrunner/rest/api/stop`

### Poslednjih `n` linija loga za odgovarajuci `guid`. Ako je n=0, preuzima se ceo log
`curl -H "Content-Type: application/json" -X GET http://147.91.200.5:8081/pakrunner/rest/api/logtail/3333-4444/4`

### Preuzimanje log fajla za `guid`
`curl -H "Content-Type: application/json" -X GET http://147.91.200.5:8081/pakrunner/rest/api/logdownload/3333-4444`

### Preuzmi rezultate u zip arhivi
`curl -d '{"guid":"3333-4444", "files":["proba.sh","pak.log"]}' -H "Content-Type: application/json" -X POST http://147.91.200.5:8081/pakrunner/rest/api/getresults --output rezultati.zip`

### Uklanjanje posla
`curl -H "Content-Type: application/json" -X GET http://147.91.200.5:8081/pakrunner/rest/api/remove/3333-4444/`

### Brisanje svih poslova
`curl -H "Content-Type: application/json" -X GET http://147.91.200.5:8081/pakrunner/rest/api/removeall`

### Upload zip-a i raspakovavanje u radni direktorijum taska `guid`
`curl -F 'file=@proba.zip' -F 'guid=3333-1111' -X POST http://147.91.200.5:8081/pakrunner/rest/api/uploadzip`

### Kopiranje fajla iz podfoldera u radni folder taska. Navodi se relativna putanja i ciljno ime fajla
`curl -d '{"guid":"3333-1111", "path":"L1/ttt.txt", "name":"ttt-kopija.txt"}' -H "Content-Type: application/json" -X POST http://147.91.200.5:8081/pakrunner/rest/api/localcopy`

### Kopiranje fajla iz radnog direktorijuma `guidsrc` u direktorijum `guiddest`
`curl -d '{"guidsrc":"3333-1111", "guiddest":"3333-2222", "namesrc":"pom.xml", "namedest":"pom.xml"}' -H "Content-Type: application/json" -X POST http://147.91.200.5:8081/pakrunner/rest/api/copyfiletasktotask`

### Brisanje fajla iz radnog direktorijuma (može i relativna putanja)
`curl -d '{"guid":"3333-1111", "path":"ttt.txt"}' -H "Content-Type: application/json" -X POST http://147.91.200.5:8081/pakrunner/rest/api/removefile`

### Preimenovanje fajla u radnom direktorijumu (može i relativna putanja)
`curl -d '{"guid":"3333-1111", "pathold":"Ulaz.csv", "pathnew":"Ulaz1.csv"}' -H "Content-Type: application/json" -X POST http://147.91.200.5:8081/pakrunner/rest/api/renamefile`

### Listing direktorijuma. Ako je `path` prazan, lista se radni direktorijum `guid`. Vraca posebno niz fajlova, a posebno niz direktorijuma.
`curl -d '{"guid":"3333-4444", "path":"/L1"}' -H "Content-Type: application/json" -X POST http://147.91.200.5:8081/pakrunner/rest/api/listfiles`


