# Rastreabilidade — Projeto II (ECM251)

Este documento transforma os requisitos mínimos da especificação do Projeto II em itens verificáveis no Location Tracking System. A especificação do professor é a fonte de prioridade; esta tabela acompanha a implementação e as evidências para a apresentação.

| Requisito | Como será atendido | Evidência prevista | Estado |
|---|---|---|---|
| Grupo de 3 a 6 alunos da mesma divisão | Confirmar e registrar os integrantes e divisão na documentação final | Capa/identificação do trabalho | Pendente de dados do grupo |
| Aplicativos Cliente e Servidor em Java, usando recursos da disciplina | Cliente Swing e servidor TCP em Java; Gson e demais dependências externas são permitidos pelo professor desde que preservem os requisitos | `pom.xml`, código e apresentação | Código implementado; validação pendente |
| MySQL obrigatório | MySQL 8+ como único SGBD; script versionado em `database/schema.sql` | Script e execução do banco | Esquema implementado; validar no MySQL |
| Cliente executado localmente | Distribuir e executar o cliente no computador local do usuário | Demonstração na máquina local | Configuração prevista; demonstração pendente |
| Servidor local ou remoto/nuvem | Executar localmente durante desenvolvimento; planejar EC2 para apresentação | Configuração e demonstração | Planejado: AWS |
| Banco local ou remoto/nuvem | MySQL local ou RDS for MySQL com acesso privado do servidor | Configuração e demonstração | Planejado: AWS |
| Servidor conecta via Driver JDBC ao MySQL | DAOs JDBC para consultas e operações de viagem | DAO e operação persistida | Código implementado; validação pendente |
| Usuário opera o Cliente somente por GUI | Fluxos de consulta e motorista no cliente Swing; utilitário de criação de conta é administrativo | Demonstração sem comandos de console do usuário | GUI implementada; demonstração pendente |
| GUI desenvolvida pelos alunos, sem gerador automático | Telas Swing construídas manualmente | Fontes Swing | Código implementado |
| `JTable`, `JComboBox`, `JMenu` e diálogos | Consulta em tabelas, filtros, menus e diálogos de ações | Demonstração de cada componente | Código implementado |
| Manipulação de senhas e datas | `JPasswordField`, hash de senha no servidor e filtros com datas | Código e demonstração | Código implementado |
| Internacionalização em pelo menos 5 idiomas | Resource bundles para português, inglês, espanhol, francês e alemão | Seletor de idioma e mudança de tela | Código implementado |
| Acesso a arquivos texto | Abrir arquivo UTF-8 e exportar relatório de texto pelo cliente | Arquivo lido/produzido e demonstração | Código implementado |
| Criptografia | AES-GCM para exportação; PBKDF2-HMAC-SHA-256 para senhas | Implementação e demonstração | Código implementado |
| Java Sockets e Threads | Cliente e servidor TCP com JSON UTF-8 por linha; threads no servidor e `SwingWorker` no cliente | Execução integrada | Código implementado; validação pendente |
| Acompanhamento de localização do produto | O cliente registra coordenadas e atualiza a listagem periodicamente; fonte GPS automática depende do dispositivo/API escolhidos | Demonstração de uma viagem e de coordenadas recentes | Registro manual implementado; captura GPS pendente |
| Documentação técnica em formato ABNT | Produzir trabalho completo com seções, referências, figuras/tabelas e formatação acadêmica | Documento final revisado | Pendente |
| Apresentação PowerPoint | Apresentar processo de desenvolvimento, arquitetura, funcionalidades e demonstração | Arquivo PPTX | Pendente |
| Entrega e apresentação em 01/12/2026 | Preparar pacote final com fonte, documentação e slides; confirmar submissão no CanvasLMS | Checklist de entrega | Prazo da especificação |
| Relatar problemas aos professores para decisão | Registrar bloqueios que dependam de decisão da disciplina e encaminhar ao grupo/professor | Registro na ata/documentação do grupo | Contínuo |

## Fluxo dos novos clientes

| Requisito | Evidência | Estado |
|---|---|---|
| Aplicações separadas de colaborador e passageiro | `client.driver.DriverApp`, `client.passenger.PassengerApp`; `ClientApp` preservado | Implementado; depende de servidor/MySQL configurados |
| Estado restaurável e reserva exclusiva | Campos operacionais em `users`, transações JDBC e índices únicos | Implementado; aplicar migração em instalações existentes |
| Localização periódica sem inventar GPS | `LocationProvider`, `ManualLocationProvider`, intervalo configurável | Coordenadas manuais implementadas; GPS real pendente |
| Mapa e ETA | `MapPanel`, `RoutesEtaClient` | Implementado; requer chaves e coordenadas confirmadas |
| Consulta de espera e horários | Novas operações do protocolo e tabela Swing | Implementado; validar integração com MySQL |
| Testes | `src/test/java` | Testes unitários de protocolo/entrada manual; concorrência e persistência requerem MySQL e permanecem pendentes |

## Decisões técnicas compatíveis

- Gson é usado para processar JSON; o professor confirmou que dependências externas são permitidas se os requisitos da disciplina forem mantidos.
- O protocolo permanece JSON sobre Java Sockets, conforme a decisão do projeto.
- O cliente é local e Swing; a nuvem é reservada ao servidor e ao MySQL.
- TLS 1.2/1.3 está disponível para o socket por configuração; TLS JDBC e provisionamento dos certificados ainda precisam ser concluídos para uso AWS.
- Nenhum recurso AWS deve ser criado sem revisão de custo, rede e credenciais pelo grupo.
