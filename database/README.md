# Database — Location Tracking System

## 1. Sobre

Este diretório contém os arquivos relacionados ao banco de dados MySQL do Location Tracking System.

O banco é responsável por armazenar usuários, veículos, viagens, localizações e horários do transporte circular.

## 2. Requisitos

- MySQL Server 8.0 ou superior.
- MySQL Workbench ou cliente MySQL equivalente.

## 3. Criando o banco

O script `schema.sql` cria automaticamente o banco `location_tracking`, caso ele ainda não exista, e cria suas tabelas.

### Pelo MySQL Workbench

1. Abra o MySQL Workbench.
2. Conecte-se à instância MySQL local.
3. Abra o arquivo `database/schema.sql`.
4. Execute o script.
5. Atualize a lista de schemas.
6. Verifique se o banco `location_tracking` e suas tabelas foram criados.

Para carregar horários fictícios sem dados pessoais, execute `database/sample-data.sql` depois do schema.

### Pelo terminal

Execute:

```bash
mysql -u root -p < database/schema.sql
```

### Atualizar uma instalação existente

Faça backup, execute `migration_001_preflight.sql` e confirme que as três consultas não retornam linhas; identifique manualmente os tipos ainda não mapeados. Depois execute uma única vez `migration_001_operational_states.sql`. A migração preserva viagens e horários, mantém os códigos `CAMPUS`/`CENTRO`, recupera reservas para viagens ativas e normaliza tipos conhecidos (`BUS`→`ONIBUS`, rótulos de micro-ônibus→`MICRO_ONIBUS`). Não reaplique a migração.

Digite a senha quando solicitado.

## 4. Estrutura das tabelas

| Tabela | Finalidade |
|---|---|
| `users` | Usuários e motoristas |
| `vehicles` | Veículos do transporte |
| `trips` | Viagens realizadas |
| `locations` | Coordenadas registradas durante as viagens |
| `schedules` | Horários do transporte |

## 5. Relacionamentos

- `users` → `trips`: um usuário pode estar associado a várias viagens.
- `vehicles` → `trips`: um veículo pode participar de várias viagens.
- `trips` → `locations`: uma viagem pode possuir vários registros de localização.
- `vehicles` → `schedules`: um veículo pode possuir vários horários cadastrados.
- `users.current_vehicle_id` → `vehicles.id`: reserva exclusiva durante `WAITING` ou `ON_TRIP`.

`current_status` aceita `AVAILABLE`, `WAITING` e `ON_TRIP`. Um índice único protege a reserva; índices únicos gerados em `trips` impedem viagens ativas duplicadas por motorista ou veículo.

## 6. Regras importantes

- Senhas devem ser armazenadas como hashes seguros, nunca em texto puro.
- O campo `role` identifica o tipo de usuário.
- Uma viagem ativa utiliza o status `ACTIVE`.
- Ao finalizar uma viagem, o status deve ser atualizado para `FINISHED`.
- Cada registro de localização deve estar associado a uma viagem.
- As coordenadas devem respeitar os limites geográficos válidos.
- Operações de escrita e leitura devem utilizar PreparedStatement através dos DAOs.
- A URL JDBC de exemplo configura a sessão MySQL em UTC para alinhar os campos `TIMESTAMP` com os instantes ISO-8601 do protocolo.

A aplicação deverá controlar, na camada de serviços, se um veículo ou motorista já possui uma viagem ativa antes de iniciar outra.

## 7. Dados de teste

Dados fictícios poderão ser inseridos durante o desenvolvimento para testar consultas e operações.

Não utilizar informações pessoais reais de motoristas ou usuários nos testes.

## 8. Responsabilidade

O integrante responsável pelo DataManager deverá:

- Validar o modelo de dados.
- Revisar os relacionamentos.
- Manter o script `schema.sql` atualizado.
- Implementar os DAOs.
- Documentar alterações na estrutura do banco.
- Garantir compatibilidade entre o banco e os objetos do pacote `model`.
