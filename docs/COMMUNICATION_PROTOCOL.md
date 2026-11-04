# Protocolo de Comunicação — Location Tracking System

## 1. Objetivo

Este documento define o padrão de comunicação entre o Client e o Server do Location Tracking System.

O objetivo é garantir que os dois componentes utilizem os mesmos formatos de mensagens, nomes de operações e estruturas de resposta, permitindo o desenvolvimento independente das duas partes.

## 2. Tecnologia de comunicação

A comunicação será realizada utilizando:

- **Java Sockets:** estabelecimento da conexão TCP.
- **JSON:** formato de representação das mensagens.
- **UTF-8:** codificação dos caracteres.
- **Uma mensagem por linha:** cada mensagem JSON será finalizada com `\n`.

O servidor poderá atender vários clientes simultaneamente, utilizando uma Thread independente para cada conexão.

### 2.1 Formato geral

Toda requisição deverá seguir esta estrutura:

```json
{
  "requestId": "123",
  "operation": "NOME_DA_OPERACAO",
  "data": {
    "campo": "valor"
  }
}
```

Campos:

| Campo | Descrição |
|---|---|
| `requestId` | Identificador da requisição, utilizado para relacionar a resposta à solicitação original |
| `operation` | Nome da operação solicitada |
| `data` | Objeto contendo os parâmetros da operação |

O `requestId` deverá ser enviado como string e repetido na resposta correspondente.

### 2.2 Formato das respostas

Resposta de sucesso:

```json
{
  "requestId": "123",
  "status": "OK",
  "data": {
    "message": "Operation completed"
  }
}
```

Resposta de erro:

```json
{
  "requestId": "123",
  "status": "ERROR",
  "error": {
    "code": "INVALID_CREDENTIALS",
    "message": "Invalid email or password"
  }
}
```

Campos:

| Campo | Descrição |
|---|---|
| `requestId` | Identificador da requisição original |
| `status` | Resultado da operação: `OK` ou `ERROR` |
| `data` | Informações retornadas em caso de sucesso |
| `error` | Código e descrição do erro |

Em cada resposta, deverá existir apenas `data` ou `error`, conforme o resultado.

## 3. Operações disponíveis

### 3.1 LOGIN

Autentica um usuário no sistema.

Requisição:

```json
{
  "requestId": "1",
  "operation": "LOGIN",
  "data": {
    "email": "motorista@email.com",
    "password": "senha"
  }
}
```

Resposta de sucesso:

```json
{
  "requestId": "1",
  "status": "OK",
  "data": {
    "userId": 10,
    "name": "Motorista",
    "role": "DRIVER",
    "token": "session-token"
  }
}
```

O servidor deverá validar as credenciais e retornar os dados necessários para identificar o usuário autenticado.

A senha nunca deverá ser armazenada em texto puro no banco de dados. A autenticação deverá utilizar senhas protegidas por hash seguro.

### 3.2 START_TRIP

Inicia uma nova viagem.

Requisição:

```json
{
  "requestId": "2",
  "operation": "START_TRIP",
  "data": {
    "vehicleId": 5,
    "direction": "CAMPUS"
  }
}
```

Resposta