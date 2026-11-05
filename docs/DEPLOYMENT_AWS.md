# Plano de implantação AWS

O enunciado permite executar o servidor e o MySQL localmente, em ambiente virtual ou na nuvem. O cliente Swing permanece instalado e executado localmente no computador do usuário.

## Arquitetura planejada

```text
Cliente Swing local
        │ Java Sockets + JSON/UTF-8
        ▼
Amazon EC2 — servidor Java
        │ JDBC/MySQL (rede privada)
        ▼
Amazon RDS for MySQL
```

- **EC2:** hospeda o processo `br.edu.maua.server.Server`.
- **RDS for MySQL:** hospeda o mesmo SGBD e esquema MySQL do ambiente local.
- **Cliente:** conecta ao DNS/IP público do servidor pela porta configurada em `SERVER_PORT`.
- **Banco:** deve permanecer em sub-redes privadas e aceitar conexões MySQL apenas do grupo de segurança da EC2.
- **Socket:** o código aceita TLS 1.2/1.3 quando `SERVER_TLS_ENABLED=true`; o cliente valida a cadeia e o nome DNS do certificado usando o truststore da JVM.

## Regras de rede

1. A regra de entrada do servidor deve permitir a porta TCP configurada somente a partir dos endereços de rede necessários para a demonstração.
2. A regra de entrada do RDS deve permitir TCP 3306 somente a partir do grupo de segurança da instância EC2.
3. Não disponibilizar a porta 3306 publicamente.
4. Usar credenciais próprias da aplicação, distintas da conta administrativa do banco, e não incluí-las no Git.

## Preparação pendente antes de publicar

- Configurar um keystore PKCS12 com certificado confiável cujo SAN corresponda ao DNS do servidor, além de `SERVER_TLS_ENABLED=true` no servidor e cliente.
- Configurar TLS para JDBC com validação do certificado do MySQL.
- Externalizar segredos da instância e limitar permissões de acesso.
- Definir região, tamanho e duração dos recursos com o grupo e revisar a estimativa de custos antes de criar EC2/RDS.
- Fazer uma prova de conexão com a rede e o MySQL antes de cadastrar dados reais.

Este documento descreve a arquitetura planejada; nenhum recurso AWS foi provisionado. O EC2 pode conectar-se ao RDS na mesma VPC, mantendo o banco privado, conforme a documentação do Amazon RDS.
