# pakrunner

*Pakrunner* je REST servis za kontrolu i monitoring dugotrajnih proračuna. 

## Primeri poziva

### Pokretanje proračuna
curl -d '{"EBeton":4.4E+04}' -H "Content-Type: application/json" -X POST http://147.91.200.5:8081/pakrunner/rest/api/start

### Da li proračun radi?
curl -H "Content-Type: application/json" -X GET http://147.91.200.5:8081/pakrunner/rest/api/isrunning

### Zaustavljanje
curl -d '' -H "Content-Type: application/json" -X POST http://147.91.200.5:8081/pakrunner/rest/api/stop

### Poslednjih N linija loga (stdout)
curl -d '' -H "Content-Type: application/json" -X GET http://147.91.200.5:8081/pakrunner/rest/api/logtail/4

### ZIP-ovani rezultati (fajl 'rezultati.zip')
curl -d '' -H "Content-Type: application/json" -X GET http://147.91.200.5:8081/pakrunner/rest/api/getresults
