# Location Tracking System

Sistema de acompanhamento em tempo real do Transporte Circular do Instituto Mauá de Tecnologia.

## 1. Sobre o projeto

O Location Tracking System é uma aplicação Cliente-Servidor desenvolvida em Java, cujo objetivo é permitir que alunos e demais usuários acompanhem a localização dos veículos do Transporte Circular do Instituto Mauá de Tecnologia.

O projeto surgiu a partir de pesquisas de satisfação realizadas na disciplina de Estatística, nas quais uma das principais sugestões dos participantes foi a implementação de uma plataforma para acompanhamento do transporte em tempo real.

O sistema permitirá:

- Acompanhamento da localização dos veículos em tempo real.
- Autenticação de motoristas.
- Compartilhamento da localização durante as viagens.
- Consulta dos veículos em operação.
- Consulta de horários e itinerários do transporte.
- Interface disponível em diferentes idiomas.

## 2. Tecnologias utilizadas

| Tecnologia | Finalidade |
|---|---|
| Java | Desenvolvimento do cliente e servidor |
| Java Swing | Interfaces gráficas |
| Java Sockets | Comunicação Cliente-Servidor |
| Threads | Atendimento simultâneo de clientes |
| Gson | Serialização e leitura do protocolo JSON |
| MySQL | Banco de dados |
| JDBC | Comunicação com o banco |
| Maven | Gerenciamento do projeto e dependências |
| AWS | Hospedagem da aplicação |

O cliente e o servidor trocam objetos JSON em linhas UTF-8 usando Gson, sobre Java Sockets.
Gson é permitido pelo professor; outras dependências permanecem subordinadas aos requisitos do trabalho.

## 3. Arquitetura do sistema

O projeto utiliza uma arquitetura Cliente-Servidor organizada em camadas.

### Client
Responsável pelas interfaces gráficas e interação com os usuários.

### Server
Responsável por receber requisições dos clientes, gerenciar conexões, processar operações e aplicar as regras de negócio.

### DataManager
Responsável pelas operações de persistência, consultas e manipulação dos dados no MySQL.

### Model
Contém as classes que representam as entidades do sistema.

### Common
Contém estruturas compartilhadas entre cliente e servidor, incluindo o protocolo de comunicação.

## 4. Estrutura do projeto

```text
location-tracking-system/
│
├── src/
│   ├── main/
│   │   ├── java/br/edu/maua/
│   │   │   ├── client/
│   │   │   │   ├── interfaces/
│   │   │   │   └── communication/
│   │   │   │
│   │   │   ├── server/
│   │   │   │   ├── services/
│   │   │   │   ├── Server.java
│   │   │   │   ├── ClientHandler.java
│   │   │   │   └── RequestHandler.java
│   │   │   │
│   │   │   ├── datamanager/
│   │   │   │   ├── dao/
│   │   │   │   └── DatabaseConnection.java
│   │   │   │
│   │   │   ├── model/
│   │   │   └── common/
│   │   │       └── protocol/
│   │   │
│   │   └── resources/
│   │       └── languages/
│   │
│   └── test/
│       └── java/
│
├── database/
│   ├── schema.sql
│   └── README.md
│
├── docs/
│   ├── ARCHITECTURE.md
│   ├── COMMUNICATION_PROTOCOL.md
│   └── DEVELOPMENT_GUIDE.md
│
├── .gitignore
├── .env.example
├── pom.xml
└── README.md
```

## 5. Configuração do ambiente

### Requisitos

- JDK 21 ou superior.
- Apache Maven 3.10.0.
- MySQL Server 8.0.
- Visual Studio Code.
- Extension Pack for Java.
- Git.

### Clonar o repositório

```bash
git clone URL_DO_REPOSITORIO
cd location-tracking-system
```

### Compilar o projeto

```bash
mvn clean compile
```

### Configurar o banco de dados

Criar o banco MySQL:

```sql
CREATE DATABASE location_tracking;
```

Execute `database/schema.sql` em uma instalação nova. Para atualizar uma instalação criada com o schema anterior, siga a migração descrita em `database/README.md`.

### Configurar variáveis de ambiente

No PowerShell:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/location_tracking?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"
$env:DB_USER="root"
$env:DB_PASSWORD="SUA_SENHA"
$env:SERVER_HOST="localhost"
$env:SERVER_PORT="5000"
$env:SERVER_TLS_ENABLED="false"
```

As credenciais não devem ser adicionadas ao código-fonte ou ao GitHub.

### Executar localmente no VS Code

1. Crie o schema executando `database/schema.sql` e, para horários de demonstração, `database/sample-data.sql`.
2. Configure as variáveis de ambiente em um terminal/perfil de execução local.
3. Execute `br.edu.maua.server.Server`.
4. Execute `br.edu.maua.datamanager.DriverAccountSetup` para cadastrar um motorista pela tela Swing. A senha é gravada somente como hash.
5. Execute `br.edu.maua.client.driver.DriverApp` para o colaborador ou `br.edu.maua.client.passenger.PassengerApp` para o passageiro. `br.edu.maua.client.ClientApp` permanece disponível como interface legada.

Para uma conexão remota, configure `SERVER_TLS_ENABLED=true` no cliente e no servidor. O servidor exige um keystore PKCS12 (`SERVER_TLS_KEYSTORE_PATH` e `SERVER_TLS_KEYSTORE_PASSWORD`) com certificado cujo nome corresponda ao DNS usado pelo cliente. Em desenvolvimento local, a configuração de exemplo usa TCP sem TLS.

No VS Code, os três pontos de entrada podem ser iniciados pelas classes Java correspondentes. A tela de criação de conta é apenas para configuração administrativa; o usuário final opera o cliente pela interface Swing.

## 6. Divisão de responsabilidades

| Integrante | Responsabilidade |
|---|---|
| DB | Modelagem do banco, scripts SQL, JDBC e DAOs |
| Server | Servidor Java, Sockets, Threads, serviços e AWS |
| Client | Interfaces gráficas, comunicação e interação com usuários |

As classes `Model` e `Common` são compartilhadas entre os integrantes e suas alterações devem ser discutidas antes da implementação.

## 7. Comunicação Cliente-Servidor

A comunicação será realizada através de Java Sockets.

Cada conexão será gerenciada por uma Thread independente, permitindo o atendimento simultâneo de múltiplos clientes.

Operações previstas:

- LOGIN
- START_TRIP
- UPDATE_LOCATION
- GET_ACTIVE_VEHICLES
- GET_SCHEDULE
- END_TRIP

A especificação detalhada do protocolo estará disponível em `docs/COMMUNICATION_PROTOCOL.md`.

## 8. Controle de versão

O desenvolvimento será realizado através do Git e GitHub.

Branches principais:

- `main`: versão estável.
- `develop`: integração das funcionalidades.
- `feature/server`: desenvolvimento do servidor.
- `feature/database`: desenvolvimento do banco.
- `feature/client`: desenvolvimento do cliente.

As funcionalidades deverão ser integradas através de Pull Requests.

## 9. Status do desenvolvimento

- [x] Configuração inicial do Maven.
- [x] Estrutura inicial dos pacotes.
- [x] Servidor TCP aceita conexões concorrentes usando Threads.
- [x] Contrato JSON por linha documentado e processado com Gson.
- [x] Instalação do MySQL Server.
- [x] Esquema MySQL inicial, DAOs e consultas JDBC para veículos e horários.
- [x] Autenticação de motorista, início/fim de viagem e registro de coordenadas implementados.
- [x] Cliente Swing com atualização de veículos/horários e ações do motorista.
- [x] Componentes Swing exigidos, cinco idiomas, leitura/escrita de texto e exportação criptografada implementados.
- [ ] Captura automática de localização de um dispositivo GPS (a interface atual recebe coordenadas do motorista).
- [ ] Compilação e demonstração integrada (ainda não verificadas neste ambiente).
- [ ] Revisão final do modelo e dos dados de demonstração.
- [ ] Documentação técnica completa em formato ABNT e apresentação PowerPoint.
- [ ] Configuração de certificados TLS e JDBC TLS para uso remoto.
- [ ] Implantação AWS (ver `docs/DEPLOYMENT_AWS.md`); nenhum recurso foi criado.
- [ ] Apresentação e entrega em 01/12/2026.

O código assinalado como implementado ainda precisa de compilação e demonstração com MySQL. Esta cópia de trabalho ainda não foi verificada em execução.

O estado por requisito da especificação da disciplina está acompanhado em
[`docs/REQUIREMENTS_TRACEABILITY.md`](docs/REQUIREMENTS_TRACEABILITY.md).

### Clientes do projeto

- O cliente do colaborador recupera estado, reserva veículo, declara/cancela espera e inicia/encerra viagens.
- O cliente do passageiro mostra veículos, mapa estático, espera e consulta de horários.
- A localização disponível é informada manualmente e identificada como tal; nenhuma coordenada é inventada.
- Maps/ETA exigem `MAPS_STATIC_API_KEY`, `GOOGLE_MAPS_ROUTES_API_KEY` e coordenadas confirmadas em `.env.example`. Sem configuração, as funções restantes continuam disponíveis.

Integração baseada nas documentações oficiais [Maps Static API](https://developers.google.com/maps/documentation/maps-static/start) e [Routes API](https://developers.google.com/maps/documentation/routes/compute_route_directions).

## 10. Objetivo

Desenvolver uma solução funcional, organizada e escalável que melhore a experiência dos usuários do Transporte Circular do Instituto Mauá de Tecnologia, oferecendo acesso às informações do transporte e acompanhamento dos veículos em operação.
