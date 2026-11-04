
package br.edu.maua.server;

import br.edu.maua.common.protocol.Protocol;

public class RequestHandler {

    public String process(String request) {

        String[] parts = request.split("\\|", -1);

        String command = parts[0];

        switch (command) {

            case Protocol.LOGIN:
                return Protocol.ERROR + "|Login ainda nao implementado";

            case Protocol.START_TRIP:
                return Protocol.ERROR + "|Inicio de viagem ainda nao implementado";

            case Protocol.UPDATE_LOCATION:
                return Protocol.ERROR + "|Localizacao ainda nao implementada";

            case Protocol.GET_ACTIVE_VEHICLES:
                return Protocol.ERROR + "|Consulta ainda nao implementada";

            case Protocol.GET_SCHEDULE:
                return Protocol.ERROR + "|Horarios ainda nao implementados";

            case Protocol.END_TRIP:
                return Protocol.ERROR + "|Encerramento ainda nao implementado";

            default:
                return Protocol.ERROR + "|Comando desconhecido";
        }
    }
}
