# pakrunner

*Pakrunner* je REST servis za kontrolu i monitoring dugotrajnih proračuna. 

## Primeri poziva

### Pokretanje proračuna
curl -d '{"guid":"3333-4444", "command":"./proba.sh"}' -H "Content-Type: application/json" -X POST http://147.91.200.5:8081/pakrunner/rest/api/start

### Da li proračun radi?
curl -H "Content-Type: application/json" -X GET http://147.91.200.5:8081/pakrunner/rest/api/isrunning/3333-4444

### Zaustavljanje
curl -d '{"guid":"3333-4444"}' -H "Content-Type: application/json" -X POST http://147.91.200.5:8081/pakrunner/rest/api/stop

### Poslednjih N linija loga za odgovarajuci GUID. Ako se stavi '0', preuzima se ceo log
curl -H "Content-Type: application/json" -X GET http://147.91.200.5:8081/pakrunner/rest/api/logtail/3333-4444/4

### Preuzimanje log fajla za GUID
curl -d '' -H "Content-Type: application/json" -X GET http://147.91.200.5:8081/pakrunner/rest/api/logdownload/3333-4444







### ZIP-ovani rezultati (fajl 'rezultati.zip')
curl -d '' -H "Content-Type: application/json" -X GET http://147.91.200.5:8081/pakrunner/rest/api/getresults
