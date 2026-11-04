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
| MySQL | Banco de dados |
| JDBC | Comunicação com o banco |
| Maven | Gerenciamento do projeto e dependências |
| AWS | Hospedagem da aplicação |

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

- JDK 26 (versão atualmente utilizada no ambiente inicial de desenvolvimento).
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

Executar o script `database/schema.sql` após sua disponibilização.

### Configurar variáveis de ambiente

No PowerShell:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/location_tracking"
$env:DB_USER="root"
$env:DB_PASSWORD="SUA_SENHA"
```

As credenciais não devem ser adicionadas ao código-fonte ou ao GitHub.

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
- [x] Servidor TCP funcional.
- [x] Comunicação básica entre cliente e servidor.
- [x] Atendimento de conexões utilizando Threads.
- [x] Estrutura inicial do protocolo de comunicação.
- [x] Instalação do MySQL Server.
- [x] Integração JDBC e teste de conexão.
- [ ] Definição definitiva do protocolo.
- [ ] Modelagem e criação das tabelas.
- [ ] Implementação dos DAOs.
- [ ] Implementação da autenticação.
- [ ] Desenvolvimento das interfaces gráficas.
- [ ] Compartilhamento de localização.
- [ ] Internacionalização.
- [ ] Hospedagem na AWS.
- [ ] Testes de integração.

## 10. Objetivo

Desenvolver uma solução funcional, organizada e escalável que melhore a experiência dos usuários do Transporte Circular do Instituto Mauá de Tecnologia, oferecendo acesso às informações do transporte e acompanhamento dos veículos em operação.