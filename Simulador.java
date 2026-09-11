import java.util.PriorityQueue;

public class Simulador {

    static class Gerador {
        private long a;
        private long c;
        private long M;
        private long anterior;

        public Gerador(long a, long c, long M, long semente) {
            this.a = a;
            this.c = c;
            this.M = M;
            this.anterior = semente;
        }

        public double proximoAleatorio() {
            this.anterior = ((this.a * this.anterior) + this.c) % this.M;
            return (double) this.anterior / (double) this.M;
        }
    }

    enum TipoEvento {
        CHEGADA,
        PASSAGEM,
        SAIDA
    }

    static class Evento implements Comparable<Evento> {
        double tempo;
        TipoEvento tipo;

        public Evento(double tempo, TipoEvento tipo) {
            this.tempo = tempo;
            this.tipo = tipo;
        }

        @Override
        public int compareTo(Evento outro) {
            return Double.compare(this.tempo, outro.tempo);
        }
    }

    static class Fila {
        String nome;
        int servidores;
        int capacidade;
        double minChegada;
        double maxChegada;
        double minAtendimento;
        double maxAtendimento;
        int clientes;
        int perdas;
        double[] tempos;

        public Fila(String nome, int servidores, int capacidade, double minChegada, double maxChegada, double minAtendimento, double maxAtendimento) {
            this.nome = nome;
            this.servidores = servidores;
            this.capacidade = capacidade;
            this.minChegada = minChegada;
            this.maxChegada = maxChegada;
            this.minAtendimento = minAtendimento;
            this.maxAtendimento = maxAtendimento;
            this.clientes = 0;
            this.perdas = 0;
            this.tempos = new double[capacidade + 1];
        }

        public int status() {
            return this.clientes;
        }

        public int capacidade() {
            return this.capacidade;
        }

        public int servidores() {
            return this.servidores;
        }

        public void perda() {
            this.perdas++;
        }

        public void entra() {
            this.clientes++;
        }

        public void sai() {
            this.clientes--;
        }
    }

    static Gerador gerador;
    static int aleatoriosRestantes;
    static double tempoGlobal;
    static PriorityQueue<Evento> escalonador;
    static Fila fila1;
    static Fila fila2;

    static Double proximoAleatorio() {
        if (aleatoriosRestantes <= 0) return null;
        aleatoriosRestantes--;
        return gerador.proximoAleatorio();
    }

    static void acumulaTempo(double novoTempo) {
        double delta = novoTempo - tempoGlobal;
        fila1.tempos[fila1.status()] += delta;
        fila2.tempos[fila2.status()] += delta;
        tempoGlobal = novoTempo;
    }

    static void chegada(Evento ev) {
        if (aleatoriosRestantes > 0) {
            Double rnd = proximoAleatorio();
            if (rnd != null) {
                double proxChegada = tempoGlobal + (fila1.minChegada + (fila1.maxChegada - fila1.minChegada) * rnd);
                escalonador.add(new Evento(proxChegada, TipoEvento.CHEGADA));
            }
        }

        if (fila1.status() < fila1.capacidade()) {
            fila1.entra();
            if (fila1.status() <= fila1.servidores()) {
                if (aleatoriosRestantes > 0) {
                    Double rnd = proximoAleatorio();
                    if (rnd != null) {
                        double tempoPassagem = tempoGlobal + (fila1.minAtendimento + (fila1.maxAtendimento - fila1.minAtendimento) * rnd);
                        escalonador.add(new Evento(tempoPassagem, TipoEvento.PASSAGEM));
                    }
                }
            }
        } else {
            fila1.perda();
        }
    }

    static void passagem(Evento ev) {
        fila1.sai();
        if (fila1.status() >= fila1.servidores()) {
            if (aleatoriosRestantes > 0) {
                Double rnd = proximoAleatorio();
                if (rnd != null) {
                    double tempoPassagem = tempoGlobal + (fila1.minAtendimento + (fila1.maxAtendimento - fila1.minAtendimento) * rnd);
                    escalonador.add(new Evento(tempoPassagem, TipoEvento.PASSAGEM));
                }
            }
        }

        if (fila2.status() < fila2.capacidade()) {
            fila2.entra();
            if (fila2.status() <= fila2.servidores()) {
                if (aleatoriosRestantes > 0) {
                    Double rnd = proximoAleatorio();
                    if (rnd != null) {
                        double tempoSaida = tempoGlobal + (fila2.minAtendimento + (fila2.maxAtendimento - fila2.minAtendimento) * rnd);
                        escalonador.add(new Evento(tempoSaida, TipoEvento.SAIDA));
                    }
                }
            }
        } else {
            fila2.perda();
        }
    }

    static void saida(Evento ev) {
        fila2.sai();
        if (fila2.status() >= fila2.servidores()) {
            if (aleatoriosRestantes > 0) {
                Double rnd = proximoAleatorio();
                if (rnd != null) {
                    double tempoSaida = tempoGlobal + (fila2.minAtendimento + (fila2.maxAtendimento - fila2.minAtendimento) * rnd);
                    escalonador.add(new Evento(tempoSaida, TipoEvento.SAIDA));
                }
            }
        }
    }

    public static void imprimirRelatorioFila(Fila f, String config) {
        System.out.println("*********************************************************");
        System.out.println("Fila:   " + f.nome + " (" + config + ")");
        if (f.minChegada > 0 || f.maxChegada > 0) {
            System.out.printf("Chegadas:    %.1f ... %.1f%n", f.minChegada, f.maxChegada);
        } else {
            System.out.println("Chegadas:    100% vindos da Fila 1 (Tandem)");
        }
        System.out.printf("Atendimento: %.1f ... %.1f%n", f.minAtendimento, f.maxAtendimento);
        System.out.println("*********************************************************");
        System.out.printf("%8s%19s%26s%n", "Estado", "Tempo", "Probabilidade");
        for (int i = 0; i <= f.capacidade; i++) {
            double prob = tempoGlobal > 0 ? (f.tempos[i] / tempoGlobal) * 100.0 : 0.0;
            System.out.printf("%7d%21.4f%21.2f%%%n", i, f.tempos[i], prob);
        }
        System.out.println();
        System.out.printf("Perdas: %d%n", f.perdas);
        System.out.println();
    }

    public static void main(String[] args) {
        gerador = new Gerador(1664525L, 1013904223L, 4294967296L, 1L);
        aleatoriosRestantes = 100000;
        tempoGlobal = 0.0;
        escalonador = new PriorityQueue<>();

        fila1 = new Fila("Q1", 2, 3, 1.0, 5.0, 4.0, 5.0);
        fila2 = new Fila("Q2", 1, 5, 0.0, 0.0, 1.0, 3.0);

        escalonador.add(new Evento(2.5, TipoEvento.CHEGADA));

        while (aleatoriosRestantes > 0 && !escalonador.isEmpty()) {
            Evento evento = escalonador.poll();

            acumulaTempo(evento.tempo);

            if (evento.tipo == TipoEvento.CHEGADA) {
                chegada(evento);
            } else if (evento.tipo == TipoEvento.PASSAGEM) {
                passagem(evento);
            } else if (evento.tipo == TipoEvento.SAIDA) {
                saida(evento);
            }
        }

        imprimirRelatorioFila(fila1, "G/G/2/3");
        imprimirRelatorioFila(fila2, "G/G/1/5");

        System.out.println("=========================================================");
        System.out.printf("Tempo global da simulacao: %.4f%n", tempoGlobal);
        System.out.println("=========================================================");
    }
}
