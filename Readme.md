# Relazione finale PacMan 2.0 | Progettazione e Sviluppo del Software - A.A. 2024/2025 | Laurea in Tecnologie dei Sistemi Informatici - UNIBO

## ANALISI 

### Analisi dei Requisiti

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

### Analisi del Problema

Il dominio applicativo è quello dei videogiochi di tipo arcade a scorrimento labirintico, in cui un giocatore interagisce con un ambiente chiuso e pieno di ostacoli, con lo scopo di completare un obiettivo (raccogliere tutte le pillole) evitando minacce (i fantasmi). Il gioco si fonda su una logica a turni continui (real-time), che prevede il costante aggiornamento dello stato del gioco in risposta alle azioni del giocatore e al comportamento degli elementi autonomi (i nemici).

• Il problema da affrontare è quello di modellare un ambiente interattivo in cui:

• Il comportamento dei nemici presenti sia sufficientemente vario e sfidante da offrire un'esperienza di gioco coinvolgente.

• Il percorso del giocatore sia influenzato dalla disposizione degli oggetti nel labirinto.

• Sia garantita una progressione di difficoltà attraverso l’introduzione di elementi variabili o aggiuntivi.

• Si gestiscano in modo corretto le condizioni di gioco (inizio, pausa, fine, punteggio, numero di vite).

**Entità principali:**

• PacMan (giocatore): entità principale controllata dall’utente. Si muove liberamente nel labirinto e interagisce con gli oggetti presenti.

• Labirinto: ambiente statico che rappresenta lo scenario di gioco. È delimitato da muri, con passaggi, corridoi e intersezioni. Contiene oggetti raccoglibili e nemici.

• Dots: oggetto raccoglibile che incrementa il punteggio. Esistono dots normali (food) e speciali (powerfood).

• Fantasma (nemico): entità autonoma che si muove nel labirinto cercando di intercettare il giocatore. In condizioni normali, il contatto tra un fantasma e PacMan causa la perdita di una vita. In presenza di un powerfood, i fantasmi diventano vulnerabili.

• Punteggio: rappresenta il risultato del giocatore ed è incrementato dalla raccolta degli oggetti.

• Vita: il giocatore dispone di un numero limitato di tentativi (vite); ogni contatto con un fantasma in stato normale comporta la perdita di una vita.

**Relazioni tra entità:**

• PacMan si muove nel Labirinto, raccoglie il food e può essere colpito dai Fantasmi.

• Fantasmi si muovono autonomamente nel Labirinto.

• Il food è posizionato nel Labirinto e scompare una volta raccolto.

• La raccolta di un powerfood modifica temporaneamente il comportamento dei Fantasmi. 

```mermaid
classDiagram
    class PacMan {
        +posizione
        +viteResidue
    }

    class Fantasma {
        +posizione
        +stato (normale/vulnerabile)
    }

    class Food {
        +valore
    }

    class PowerFood {
        +effetto (vulnerabilità nemici)
    }

    class Labirinto {
        +struttura
        +oggettiPresenti
    }

    class Livello {
        +numero
        +difficoltà
    }

    class Vita {
        +conteggio
    }

    class Punteggio {
        +valoreCorrente
    }

    PowerFood --|> Food : estensione
    PacMan --> Food : raccoglie
    PacMan --> PowerFood : raccoglie
    PacMan --> Fantasma : interazione
    PacMan --> Labirinto : si muove in
    Fantasma --> Labirinto : si muove in
    Labirinto --> Livello : fa parte di
    Livello --> Vita : gestisce
    Livello --> Punteggio : aggiorna
 ```
## DESIGN

### Architettura generale
L'applicazione PacMan 2.0 è stata progettata seguendo un'architettura modulare, con una suddivisione chiara delle responsabilità tra le varie componenti. Il sistema ruota attorno a una struttura ad alto livello ispirata al pattern MVC, dove:

• Il Model gestisce lo stato del gioco (posizione dei personaggi, mappa, punteggio, ecc.).

• La View, fornita dall’ambiente JavaFX, si occupa della rappresentazione visiva del gioco.

• Il Controller, implicito nella logica di gestione degli input, traduce le interazioni dell’utente in movimenti e azioni.

Questa architettura ha consentito un’evoluzione ordinata del progetto e una gestione più chiara dei singoli aspetti funzionali, come fantasmi, frutti o pillole.

### Componenti principali
> App.java
Questo componente rappresenta il punto d’ingresso dell’applicazione. Ha il compito di:

• Avviare l’ambiente JavaFX

• Visualizzare il menu iniziale (gioca, esci, ecc.)

• Inizializzare e lanciare la schermata principale di gioco

> PacMan.java
È il cuore logico dell'applicazione. In esso è concentrata la gestione del ciclo di gioco:

• Avanzamento del tempo

• Movimento dei personaggi

• Collisioni tra entità (es. PacMan e fantasmi, pillole, frutti)

• Aggiornamento del punteggio e delle vite

Coordina inoltre le varie componenti esterne, come il labirinto o i manager dedicati.

> GameMap.java
Modella il labirinto di gioco, ovvero la struttura della mappa. Gestisce:

• La disposizione dei blocchi, delle pillole e delle pillole speciali

• Le informazioni sugli spazi percorribili

• Le interazioni tra gli elementi statici e i personaggi

> Direction.java
Questa componente rappresenta un'astrazione per la direzione del movimento, consentendo una gestione ordinata dei comandi direzionali (su, giù, sinistra, destra). Facilita la gestione dell’input e del comportamento dinamico dei personaggi.

> GhostManager.java
Gestisce il comportamento e il movimento dei fantasmi, che possono essere:

• In modalità inseguimento (cercano PacMan)

• In modalità fuga (quando è attiva una pillola speciale)

• In modalità ritorno alla base (quando vengono “mangiati”)

Gestisce anche eventuali differenze comportamentali tra fantasmi.

> FruitManager.java
Controlla la comparsa e la raccolta dei frutti bonus. I frutti appaiono in momenti specifici della partita e forniscono punti aggiuntivi se raccolti da PacMan.

> Block.java
Rappresenta un elemento del labirinto. Ogni blocco può essere:

• Una parete

• Uno spazio libero

• Una posizione contenente una pillola, frutto o altro oggetto

Questa classe viene utilizzata per strutturare il labirinto in modo modulare.

### Personalizzazioni introdotte in PacMan 2.0
Il progetto introduce alcune varianti rispetto alla versione classica:


## ARCHITETTURA

L’architettura di PacMan 2.0 segue una logica a componenti indipendenti, con chiara separazione dei ruoli tra gestione del gioco, interfaccia utente e logiche specializzate (gestione mappa, nemici, bonus).

Più in dettaglio, il sistema può essere scomposto in tre gruppi principali:

• Boundary (interfaccia utente): App

• Control (logica del gioco): PacMan

• Entity (modello del dominio): GameMap, Block, GhostManager, FruitManager, Direction

Ogni componente architetturale ricopre uno o più ruoli precisi e interagisce tramite passaggi diretti di informazioni o gestione condivisa dello stato.

### Ruoli e interazioni
**App**
• Ruolo: Boundary

• Responsabilità:

> Avvia l’interfaccia utente

> Inizializza la componente di controllo (PacMan)

Interazioni:

> Crea e invoca PacMan per iniziare la partita

> Presenta al giocatore il menu iniziale

**PacMan**
• Ruolo: Control

• Responsabilità:

> Coordina l’intero ciclo di gioco

> Smista input e aggiorna lo stato

• Interazioni:

> Richiede lo stato attuale da GameMap

> Controlla i fantasmi tramite GhostManager

> Verifica la presenza di bonus con FruitManager

> Usa Direction per interpretare l’input del giocatore

**GameMap**
• Ruolo: Entity

• Responsabilità:

> Rappresenta la struttura del labirinto

> Fornisce accesso ai Block

• Interazioni:

> Espone informazioni necessarie a PacMan

> Contiene e organizza Block in forma di griglia

**GhostManager** 
• Ruolo: Entity

• Responsabilità:

> Gestisce tutti i fantasmi e il loro stato

• Interazioni:

> Riceve aggiornamenti e direttive da PacMan

**FruitManager**
• Ruolo: Entity

• Responsabilità:

> Controlla l’apparizione e la raccolta dei frutti bonus

• Interazioni:

> Consultato da PacMan per verificare interazioni con frutti

**Direction**
• Ruolo: Entity

• Responsabilità:

> Fornisce un’astrazione semplice per i movimenti

• Interazioni:

> Usato da PacMan per interpretare i comandi del giocatore



