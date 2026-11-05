# Arquitetura do Sistema — Location Tracking System

## 1. Visão geral

O Location Tracking System utiliza uma arquitetura Cliente-Servidor dividida em camadas. Cada camada possui responsabilidades específicas, reduzindo o acoplamento entre os componentes e facilitando o desenvolvimento paralelo.

O fluxo geral da aplicação é:

1. O usuário interage com a interface gráfica do cliente.
2. O cliente envia uma requisição ao servidor através de Java Sockets.
3. O servidor recebe e interpreta a requisição.
4. A camada de serviços executa as regras de negócio.
5. Quando necessário, os serviços solicitam operações ao DataManager.
6. O DataManager acessa o MySQL através do JDBC.
7. O resultado retorna pelo mesmo caminho até o cliente.

## 2. Organização das camadas

### 2.1 Client

**Responsável:** integrante Client.

Contém tudo relacionado à aplicação executada pelo usuário.

Pacotes:

- `client.interfaces`: janela Swing, tabelas, filtros e diálogos escritos manualmente.
- `client.communication`: cliente de socket TCP que envia e recebe JSON em UTF-8.
- `client.ClientApp`: ponto de entrada gráfico do cliente.

Responsabilidades:

- Exibir telas de login.
- Exibir informações dos veículos.
- Apresentar a localização dos circulares.
- Permitir que motoristas iniciem e encerrem viagens.
- Enviar atualizações de localização.
- Permitir consulta de horários.
- Apresentar mensagens de erro e confirmação.

O Client não deve acessar o banco de dados diretamente.

### 2.2 Server

**Responsável:** integrante Server.

É o núcleo da aplicação. Recebe requisições dos clientes e coordena sua execução.

Classes principais:

- `Server.java`: inicia o servidor e aguarda conexões.
- `ClientHandler.java`: gerencia a comunicação com um cliente conectado.
- `RequestHandler.java`: interpreta as requisições e direciona cada operação.
- `server.services`: contém autenticação, sessões, regras de viagem e consultas de transporte.

Responsabilidades:

- Aceitar conexões TCP.
- Gerenciar múltiplos clientes através de Threads.
- Interpretar mensagens recebidas.
- Validar operações solicitadas.
- Coordenar os serviços.
- Retornar respostas aos clientes.

O servidor não deve executar comandos SQL diretamente. O acesso aos dados deve ocorrer através dos componentes do DataManager.

### 2.3 DataManager

**Responsável:** integrante DB.

É a camada responsável pela persistência dos dados.

Pacotes:

- `datamanager`: conexão com o banco.
- `datamanager.dao`: `UserDao`, `TripDao`, `VehicleDao` e `ScheduleDao`.

Responsabilidades:

- Estabelecer conexão com o MySQL através do JDBC.
- Executar consultas e alterações.
- Inserir registros.
- Atualizar registros.
- Consultar informações.
- Excluir registros quando necessário.

Os DAOs devem esconder os detalhes de SQL das demais camadas.

### 2.4 Model

Contém as classes que representam os objetos principais do sistema.

Classes do modelo atualmente implementadas:

| Classe | Responsabilidade |
|---|---|
| `UserCredential` | Dados necessários para validar uma conta de motorista |
| `UserSession` | Sessão autenticada de curta duração mantida na memória do servidor |
| `ActiveVehicle` | Veículo em viagem e sua última coordenada |
| `ScheduleEntry` | Horário de partida de um veículo |

Essas classes poderão ser utilizadas tanto pelo Client quanto pelo Server e DataManager, quando necessário.

### 2.5 Common

Contém componentes compartilhados entre o Client e o Server.

O pacote `common.protocol` define os campos e nomes de operação usados pelo cliente e servidor. Gson serializa e interpreta os objetos JSON.

Isso evita que cada integrante implemente nomes e formatos diferentes para as mesmas operações.

## 3. Fluxo de comunicação

Exemplo: atualização da localização de um veículo.

1. O motorista inicia uma viagem no Client.
2. O Client obtém a localização do dispositivo ou recebe as coordenadas disponíveis.
3. O Client envia uma requisição `UPDATE_LOCATION`.
4. O Server recebe a mensagem através do `ClientHandler`.
5. O `RequestHandler` identifica a operação.
6. O serviço responsável valida a atualização.
7. O serviço solicita ao DAO o registro da localização.
8. O DAO grava os dados no MySQL.
9. O Server retorna uma resposta ao Client.

## 4. Modelo inicial de dados

As principais entidades previstas são:

- USER
- VEHICLE
- TRIP
- LOCATION
- SCHEDULE

Relacionamentos propostos:

- Um usuário pode estar associado a várias viagens como motorista.
- Um veículo pode participar de várias viagens.
- Uma viagem pode possuir vários registros de localização.
- Um veículo pode possuir vários horários cadastrados.

O modelo definitivo será documentado e implementado pelo integrante responsável pelo banco de dados.

## 5. Regras de dependência

Para manter a organização:

- Client comunica-se com Server através do protocolo definido.
- Server utiliza os Services para executar as regras de negócio.
- Services utilizam os DAOs para acessar os dados.
- DAOs utilizam JDBC para comunicar-se com o MySQL.
- Model contém as entidades compartilhadas.
- Common define estruturas e operações compartilhadas.

**Regra importante:** o Client nunca acessa o MySQL diretamente, e o Server não deve concentrar comandos SQL em suas classes de comunicação.

## 6. Divisão de desenvolvimento

| Integrante | Componentes |
|---|---|
| Client | `client.interfaces`, `client.communication` |
| Server | `server`, `server.services` |
| DB | `datamanager`, `datamanager.dao` |
| Todos | `model`, `common` |

Alterações em `model` e `common` devem ser discutidas entre os integrantes antes de serem incorporadas.

## 7. Evolução da arquitetura

A implementação será desenvolvida progressivamente:

1. Estabelecer comunicação entre Client e Server.
2. Definir e implementar o protocolo.
3. Implementar o modelo e o banco de dados.
4. Implementar os DAOs.
5. Implementar os serviços.
6. Integrar as interfaces gráficas.
7. Implementar o compartilhamento de localização.
8. Realizar testes de integração.
9. Preparar a aplicação para hospedagem na AWS.
