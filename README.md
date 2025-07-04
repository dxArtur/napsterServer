# Napster P2P - Compartilhamento de Arquivos com Sockets

Este projeto é uma implementação simplificada do sistema de compartilhamento de arquivos baseado no **Napster**, desenvolvida como atividade da disciplina de **Sistemas Distribuídos**.

O sistema usa **Java com sockets** para simular a comunicação entre clientes e um servidor central. Os arquivos são compartilhados entre os clientes de forma peer-to-peer (P2P), com o servidor atuando apenas como coordenador das buscas.

---

## 🧠 Visão Geral

### Componentes:

- **Servidor Napster**:
  - Escuta na porta `1234`.
  - Registra quais clientes estão conectados e quais arquivos eles possuem.
  - Responde a comandos como: `JOIN`, `CREATEFILE`, `DELETEFILE`, `SEARCH`, `LEAVE`.

- **Cliente Napster**:
  - Conecta ao servidor e envia automaticamente os arquivos da pasta `/public`.
  - Escuta em uma **porta de upload** (ex: `1235`) para responder a downloads.
  - Permite: buscar arquivos, baixar de outros clientes e sair da rede.

---

## ▶️ Como Rodar o Projeto

### 1. Compile os arquivos

```bash

# Compile o servidor
javac server\NapsterServer.java

# Compile o cliente
javac client\NapsterServer.java

# Compile o servidor
java server.NapsterServer

# Execute o cliente (passando a porta desejada)
java client.NapsterClient 1235

```
