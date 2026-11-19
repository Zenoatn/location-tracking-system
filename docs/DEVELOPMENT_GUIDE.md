# Development Guide — Location Tracking System

## Novas aplicações Swing

- Colaborador: `br.edu.maua.client.driver.DriverApp`.
- Passageiro: `br.edu.maua.client.passenger.PassengerApp`.
- Compatibilidade: `br.edu.maua.client.ClientApp` continua disponível.
- Localização: coordenada manual com envio periódico configurado por `LOCATION_UPDATE_INTERVAL_MS` (padrão 5000 ms); GPS real não está implementado.
- Mapa/estimativa: configurar `MAPS_STATIC_API_KEY`, `GOOGLE_MAPS_ROUTES_API_KEY` e coordenadas confirmadas `MAP_IMT_LAT/LNG`, `MAP_ESTACAO_LAT/LNG`. APIs oficiais: https://developers.google.com/maps/documentation/maps-static/start e https://developers.google.com/maps/documentation/routes/compute_route_directions.
- Banco existente: backup e execução única de `database/migration_001_operational_states.sql`; banco novo usa `database/schema.sql`.

## 1. Objetivo

Este documento define as regras de desenvolvimento, organização do código e integração entre os integrantes do Location Tracking System.

O objetivo é permitir que os integrantes trabalhem simultaneamente em diferentes partes do projeto, mantendo compatibilidade entre os componentes.

## 2. Preparação do ambiente

Todos os integrantes deverão utilizar:

- JDK 21 ou superior (o build está configurado com `--release 21`).
- Apache Maven 3.10.0.
- MySQL Server 8.0 ou superior.
- Git.
- Visual Studio Code com Extension Pack for Java.

Antes de começar, cada integrante deverá:

1. Instalar as ferramentas necessárias.
2. Clonar o repositório.
3. Abrir a pasta que contém o `pom.xml` no VS Code.
4. Configurar as variáveis de ambiente necessárias.
5. Executar `mvn clean compile`.
6. Confirmar que o projeto compila sem erros.

## 3. Divisão de responsabilidades

### Integrante 1 — DataManager

Responsável pelo banco de dados e persistência.

Diretórios principais:

```text
datamanager/
├── DatabaseConnection.java
└── dao/
```

Tarefas:

- Revisar o modelo de dados.
- Manter `database/schema.sql`.
- Implementar os DAOs.
- Implementar operações CRUD necessárias.
- Garantir a comunicação com MySQL através de JDBC.
- Definir como os dados serão convertidos para os objetos do pacote `model`.

Entregáveis:

- Banco de dados funcional.
- Script SQL atualizado.
- DAOs implementados.
- Testes das operações de persistência.

### Integrante 2 — Server

Responsável pela comunicação e regras de negócio.

Diretórios principais:

```text
server/
├── services/
├── Server.java
├── ClientHandler.java
└── RequestHandler.java
```

Tarefas:

- Manter o servidor TCP.
- Gerenciar conexões simultâneas.
- Implementar o processamento de requisições JSON.
- Implementar os serviços.
- Integrar os serviços aos DAOs.
- Implementar autenticação e autorização.
- Preparar a aplicação para hospedagem na AWS.

Entregáveis:

- Servidor funcional.
- Comunicação Cliente-Servidor.
- Processamento das operações.
- Integração com o banco.
- Testes de comunicação e regras de negócio.

### Integrante 3 — Client

Responsável pela interface gráfica e interação com os usuários.

Diretórios principais:

```text
client/
├── interfaces/
└── communication/
```

Tarefas:

- Desenvolver as interfaces utilizando Java Swing.
- Implementar a tela de login.
- Criar a interface do motorista.
- Criar a interface do passageiro.
- Implementar a comunicação com o servidor.
- Exibir localização e informações dos veículos.
- Implementar consulta de horários.
- Preparar a internacionalização.

Entregáveis:

- Interfaces gráficas funcionais.
- Comunicação com o servidor.
- Telas de motorista e passageiro.
- Tratamento de erros e respostas.
- Testes das funcionalidades da interface.

## 4. Classes compartilhadas

Os seguintes componentes serão utilizados por mais de um integrante:

### Model

Contém as entidades:

- `User`
- `Vehicle`
- `Trip`
- `Location`
- `Schedule`

As classes deverão representar os dados do sistema, evitando concentrar regras de negócio ou comandos SQL.

### Common

Contém:

- Constantes das operações.
- Estruturas do protocolo.
- Classes auxiliares compartilhadas.

Alterações nesses diretórios deverão ser discutidas entre os integrantes antes de serem incorporadas.

## 5. Regras de desenvolvimento

1. Cada integrante deverá trabalhar prioritariamente nos diretórios sob sua responsabilidade.
2. Não alterar arquivos de outro integrante sem comunicação prévia.
3. Não implementar operações com formatos diferentes dos definidos em `COMMUNICATION_PROTOCOL.md`.
4. Não armazenar senhas ou credenciais no código-fonte.
5. Não adicionar dependências ao `pom.xml` sem informar os demais integrantes.
6. Não realizar consultas SQL diretamente nas classes do Server ou Client.
7. Não enviar código que não compile.
8. Atualizar a documentação quando houver mudanças relevantes.

## 6. Organização do Git

O projeto utilizará as seguintes branches:

| Branch | Finalidade |
|---|---|
| `main` | Versão estável do projeto |
| `develop` | Integração das funcionalidades |
| `feature/server` | Desenvolvimento do servidor |
| `feature/database` | Desenvolvimento do banco |
| `feature/client` | Desenvolvimento do cliente |

### 6.1 Primeiro acesso

Após clonar o repositório:

```bash
git clone URL_DO_REPOSITORIO
cd location-tracking-system
git checkout develop
```

### 6.2 Desenvolvimento de uma funcionalidade

Antes de começar uma tarefa:

```bash
git checkout develop
git pull origin develop
git checkout -b feature/nome-da-funcionalidade
```

Exemplo:

```bash
git checkout -b feature/login
```

Após implementar a funcionalidade:

```bash
git add .
git commit -m "feat: implement login"
git push -u origin feature/login
```

Em seguida, criar um Pull Request no GitHub com destino à branch `develop`.

### 6.3 Integração

O código deverá ser revisado antes de ser incorporado à branch `develop`.

Depois da integração, os demais integrantes deverão atualizar suas branches para receber as mudanças.

A branch `main` deverá receber apenas versões consideradas estáveis.

## 7. Padrão de commits

Utilizar mensagens curtas e descritivas.

Exemplos:

```text
feat: implement user authentication
feat: add vehicle location update
fix: correct database connection
docs: update communication protocol
refactor: reorganize server services
test: add database connection tests
```

## 8. Critérios para concluir uma tarefa

Uma tarefa poderá ser considerada concluída quando:

- O código estiver implementado.
- O projeto compilar sem erros.
- Os testes aplicáveis forem executados.
- A comunicação com os outros componentes respeitar os contratos definidos.
- A documentação necessária estiver atualizada.
- O código estiver disponível em uma branch no GitHub.

## 9. Fluxo de integração

A integração deverá acontecer progressivamente:

1. DataManager disponibiliza os DAOs e os modelos de persistência.
2. Server integra os serviços aos DAOs.
3. Client integra suas telas ao protocolo de comunicação.
4. Os três integrantes realizam testes de integração.
5. Problemas encontrados são corrigidos nas respectivas branches.
6. A versão estável é incorporada à branch `main`.

## 10. Comunicação entre os integrantes

Antes de alterar uma classe compartilhada ou modificar o protocolo, os integrantes deverão discutir a mudança.

Os documentos abaixo são a referência oficial do projeto:

- `README.md`: visão geral e configuração.
- `docs/ARCHITECTURE.md`: arquitetura e responsabilidades.
- `docs/COMMUNICATION_PROTOCOL.md`: contrato de comunicação.
- `database/README.md`: configuração e estrutura do banco.

Em caso de divergência, os integrantes deverão atualizar a documentação antes de implementar formatos incompatíveis.
