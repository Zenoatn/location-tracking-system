# Protocolo de Comunicação — Location Tracking System

## Transporte

- Transporte TCP com Java Sockets.
- Codificação UTF-8.
- Uma mensagem JSON compacta por linha (`\n`).
- Após abrir a conexão, o cliente envia requisições e o servidor responde na mesma conexão. Não há mensagem de boas-vindas fora do protocolo.
- Cada resposta repete o `requestId` da requisição correspondente.

## Requisição

```json
{
  "requestId": "req-001",
  "operation": "GET_ACTIVE_VEHICLES",
  "data": {}
}
```

`requestId` e `operation` são strings obrigatórias; `data` deve ser um objeto JSON. O identificador deve ser único entre requisições pendentes do cliente e ter no máximo 128 caracteres.

## Resposta

Sucesso:

```json
{
  "requestId": "req-001",
  "status": "OK",
  "data": {}
}
```

Erro:

```json
{
  "requestId": "req-001",
  "status": "ERROR",
  "error": {
    "code": "INVALID_REQUEST",
    "message": "Descrição legível do erro."
  }
}
```

Uma resposta contém `data` ou `error`, nunca ambos. Se o JSON for inválido ou não contiver um `requestId` string, a resposta de erro poderá omitir `requestId`.

## Operações

### `LOGIN`

Requisição: `{ "email": "driver@example.org", "password": "senha" }`

Sucesso: `{ "userId": 10, "name": "Motorista", "role": "DRIVER", "token": "...", "activeTripId": null }`

`activeTripId` contém o identificador da viagem ativa do motorista quando houver, ou `null` caso contrário. Ele permite retomar uma viagem após fechar e reabrir o cliente.

A senha trafega apenas para autenticação e deve ser armazenada no banco como hash seguro, nunca em texto puro. O token será exigido nas operações de motorista.

### `START_TRIP`

Requisição: `{ "token": "...", "vehicleId": 5, "direction": "CAMPUS" }`

Sucesso: `{ "tripId": 101, "startedAt": "2026-11-04T12:00:00Z" }`

### `UPDATE_LOCATION`

Requisição: `{ "token": "...", "tripId": 101, "latitude": -23.6, "longitude": -46.6, "recordedAt": "2026-11-04T12:01:00Z" }`

Sucesso: `{ "recorded": true }`

Latitude deve estar entre -90 e 90; longitude entre -180 e 180. `recordedAt` é opcional; se ausente, o servidor define o horário.

### `GET_ACTIVE_VEHICLES`

Requisição: `{}`

Sucesso: `{ "vehicles": [{ "vehicleId": 5, "identification": "Circular 1", "direction": "CAMPUS", "latitude": -23.6, "longitude": -46.6, "recordedAt": "2026-11-04T12:01:00Z" }] }`

### `GET_SCHEDULE`

Requisição: `{ "direction": "CAMPUS", "dayType": "WEEKDAY" }`

`direction` e `dayType` podem ser omitidos para consultar todos os horários ativos.

Sucesso: `{ "schedules": [{ "vehicleId": 5, "departureTime": "08:30", "direction": "CAMPUS", "dayType": "WEEKDAY" }] }`

### `END_TRIP`

Requisição: `{ "token": "...", "tripId": 101 }`

Sucesso: `{ "tripId": 101, "endedAt": "2026-11-04T13:00:00Z", "status": "FINISHED" }`

## Códigos de erro

- `INVALID_REQUEST`: JSON malformado, campos obrigatórios ausentes ou inválidos.
- `UNKNOWN_OPERATION`: operação não reconhecida.
- `INVALID_CREDENTIALS`: credenciais inválidas.
- `UNAUTHORIZED`: token inválido ou ausente.
- `NOT_FOUND`: usuário, veículo, viagem ou horário inexistente.
- `CONFLICT`: motorista ou veículo já está em uma viagem ativa.
- `VALIDATION_ERROR`: dados fora do domínio aceito.
- `INTERNAL_ERROR`: falha inesperada; detalhes internos não são enviados ao cliente.
- `NOT_IMPLEMENTED`: operação ainda indisponível durante o desenvolvimento.

## Compatibilidade

Os exemplos definem o contrato compartilhado pelo cliente e servidor. Alterações devem ser refletidas neste documento e implementadas nos dois lados na mesma mudança. Datas e horários devem ser enviados em ISO-8601; instantes devem incluir fuso horário.
