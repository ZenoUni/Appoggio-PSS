# Relazione finale PacMan 2.0 | Progettazione e Sviluppo del Software - A.A. 2024/2025 | Laurea in Tecnologie dei Sistemi Informatici - UNIBO

**ANALISI**

***Analisi dei Requisiti***

L'applicazione PacMan 2.0 dovrà soddisfare i seguenti requisiti funzionali:

• Il giocatore deve poter controllare il personaggio principale (PacMan) all'interno di un labirinto.

• Il labirinto deve contenere dei "dots" (cibo) che PacMan può raccogliere per accumulare punti.

• Devono essere presenti nemici (i fantasmi), che si muovono nel labirinto secondo un comportamento definito e non.

• Se un fantasma mangia PacMan, il giocatore perde una vita.

• Il gioco termina quando:

> Il giocatore perde tutte le vite.

> Il giocatore raccoglie tutte le pillole presenti nel labirinto.

• Devono essere previste condizioni di vittoria e di sconfitta.

• Il punteggio del giocatore deve essere aggiornato in tempo reale.

• Devono essere presenti elementi di interazione dinamica, come dots speciali che modificano temporaneamente il comportamento dei fantasmi.

• Il gioco deve essere rigiocabile, ovvero il giocatore può iniziare una nuova partita una volta terminata quella corrente.

Eventuali requisiti aggiuntivi rispetto al gioco originale (PacMan 2.0):

• Devono essere introdotti "poteri" speciali per PacMan

• Aggiungere una sezione "skin" al menù iniziale.

***Analisi del Problema***

Il dominio applicativo è quello dei videogiochi di tipo arcade a scorrimento labirintico, in cui un giocatore interagisce con un ambiente chiuso e pieno di ostacoli, con lo scopo di completare un obiettivo (raccogliere tutte le pillole) evitando minacce (i fantasmi). Il gioco si fonda su una logica a turni continui (real-time), che prevede il costante aggiornamento dello stato del gioco in risposta alle azioni del giocatore e al comportamento degli elementi autonomi (i nemici).

• Il problema da affrontare è quello di modellare un ambiente interattivo in cui:

• Il comportamento dei nemici presenti sia sufficientemente vario e sfidante da offrire un'esperienza di gioco coinvolgente.

• Il percorso del giocatore sia influenzato dalla disposizione degli oggetti nel labirinto.

• Sia garantita una progressione di difficoltà attraverso l’introduzione di elementi variabili o aggiuntivi.

• Si gestiscano in modo corretto le condizioni di gioco (inizio, pausa, fine, punteggio, numero di vite).

_Entità principali:_

• PacMan (giocatore): entità principale controllata dall’utente. Si muove liberamente nel labirinto e interagisce con gli oggetti presenti.

• Labirinto: ambiente statico che rappresenta lo scenario di gioco. È delimitato da muri, con passaggi, corridoi e intersezioni. Contiene oggetti raccoglibili e nemici.

• Dots: oggetto raccoglibile che incrementa il punteggio. Esistono dots normali (food) e speciali (powerfood).

• Fantasma (nemico): entità autonoma che si muove nel labirinto cercando di intercettare il giocatore. In condizioni normali, il contatto tra un fantasma e PacMan causa la perdita di una vita. In presenza di un powerfood, i fantasmi diventano vulnerabili.

• Punteggio: rappresenta il risultato del giocatore ed è incrementato dalla raccolta degli oggetti.

• Vita: il giocatore dispone di un numero limitato di tentativi (vite); ogni contatto con un fantasma in stato normale comporta la perdita di una vita.

_Relazioni tra entità:_

• PacMan si muove nel Labirinto, raccoglie il food e può essere colpito dai Fantasmi.

• Fantasmi si muovono autonomamente nel Labirinto.

• Il food è posizionato nel Labirinto e scompare una volta raccolto.

• La raccolta di un powerfood modifica temporaneamente il comportamento dei Fantasmi. 

```mermaid
classDiagram
    class PacMan {
        <<entity>>
    }

    class Fantasma {
        <<entity>>
    }

    class Pillola {
        <<entity>>
    }

    class PillolaSpeciale {
        <<entity>>
    }

    class Labirinto {
        <<entity>>
    }

    class Livello {
        <<entity>>
    }

    class Vita {
        <<entity>>
    }

    class Punteggio {
        <<entity>>
    }

    PacMan --> Pillola : raccoglie
    PacMan --> PillolaSpeciale : raccoglie
    PillolaSpeciale --|> Pillola : estende
    PacMan --> Fantasma : può essere colpito da
    PacMan --> Labirinto : si muove in
    Fantasma --> Labirinto : si muove in
    Labirinto --> Livello : parte di
    Livello --> Vita : gestisce
    Livello --> Punteggio : aggiorna
 ```
