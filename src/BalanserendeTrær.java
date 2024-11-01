import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

public class BalanserendeTrær{
    public static void main(String[] args) {
        // Forskjellige tidstester.
        int n = 100_000; // Antall elementer
        int r = 1_000; // Antall repetisjoner
        long seed = 1829;

        System.out.println("Tester med "+ n + " elementer lagt inn, repetert "+ r+ " ganger.");
        testTre(RSTreNedenfraOpp.class, n, r, seed);
        testTre(VLRSTreNedenfraOpp.class, n, r, seed);
        testTre(VLRSTreOvenfraNed.class, n, r, seed);
        testTre(BokRSTre.class, n, r, seed);
    }

    @SuppressWarnings("unchecked")
    public static void testTre(Class cls, int n, int r, long seed) {
        try {
            Method lagtre = cls.getMethod("rstre");
            RSTre<Integer> rstre = null;
            long sum = 0;
            int høyde = 0;
            System.out.println("Resultater for tre " + cls.toString().substring(6));
            try {
                for (int i = 0; i < r; i++) {
                    rstre = (RSTre<Integer>) lagtre.invoke(cls);
                    sum += treTid(rstre::leggInnIterativ, n, seed);
                }
                System.out.println("Gjennomsnittstid (Iterativt): " + ((double) sum) / r + " ms");
                høyde = rstre.høyde();
            } catch (UnsupportedOperationException e) {
                System.out.println("Gjennomsnittstid (Iterativt): ∞ ms (Ikke implementert)");
            }
            sum = 0;
            try {
                for (int i = 0; i < r; i++) {
                    rstre = (RSTre<Integer>) lagtre.invoke(cls);
                    sum += treTid(rstre::leggInnRekursiv, n, seed);
                }
                System.out.println("Gjennomsnittstid (Rekursivt): " + ((double) sum) / r + " ms");
                høyde = rstre.høyde();
            } catch (UnsupportedOperationException e) {
                System.out.println("Gjennomsnittstid (Rekursivt): ∞ ms (Ikke implementert)");
            }
            System.out.println("Høyde på tre: " + høyde);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static long treTid(Consumer<Integer> c, int n, long seed) {
        // Legger inn n verdier i en Consumer, som kommer til å være leggInn-metoden til et tre.
        // Returnerer hvor mange millisekund det tok.
        int[] randList = randPerm(n, seed); // Tilfeldig liste
        long tic = System.currentTimeMillis();
        for (int i : randList) {
            c.accept(i);
        }
        return System.currentTimeMillis() - tic;
    }

    public static int[] randPerm(int n, long seed) {
        // Liste med ints fra 0 til n-1 i tilfeldig rekkefølge bestemt av seed
        Random r = new Random();
        r.setSeed(seed);
        int[] a = new int[n];
        for (int i = 0; i < n; ++i) a[i] = i; // fyll inn verdier 1-n
        for (int k = n-1; k > 0; --k) { // Bytt plass på tilfeldig valgte verdier
            int i = r.nextInt(k+1);
            bytt(a, i, k);
        }
        return a;
    }

    public static void bytt(int[] a, int i, int j) {
        int tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }
}

abstract class RSTre<T> { // Abstrakt klasse som implementerer alt som er felles for de forskjellige trærne
    static final boolean RØD = true; // Kunne brukt enum, men siden vi kun har to verdier, er dette enklere.
    static final boolean SORT = false;

    Node<T> rot;
    int antall;
    Comparator<? super T> sammenlikner;

    static final class Node<T> {
        Node<T> venstre, høyre;
        boolean farge; T verdi;

        Node(T verdi) {
            this.verdi = verdi; farge = RØD;
        }
    }

    public RSTre(Comparator<? super T> sammenlikner) {
        this.sammenlikner = sammenlikner; antall = 0;
    }

    public int antall() {return antall;}
    public boolean tom() {return antall == 0;}
    public int høyde() {return høyde(rot);}
    private int høyde(Node<T> n) {
        if (n == null) return -1;
        return 1+Math.max(høyde(n.venstre), høyde(n.høyre));
    }

    boolean erRød(Node<T> h) {
        return (h != null && h.farge);
    }

    void fargeskift(Node<T> h) {
        h.farge = !h.farge;
        h.venstre.farge = !h.venstre.farge;
        h.høyre.farge = !h.høyre.farge;
    }

    Node<T> venstreRoter(Node<T> h) {
        Node<T> q = h.høyre;
        h.høyre = q.venstre;
        q.venstre = h;
        q.farge = h.farge;
        h.farge = RØD;
        return q;
    }

    Node<T> høyreRoter(Node<T> h) {
        Node<T> q = h.venstre;
        h.venstre = q.høyre;
        q.høyre = h;
        q.farge = h.farge;
        h.farge = RØD;
        return q;
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ", "[", "]");
        toString(sj, rot);
        return sj.toString();
    }

    private void toString(StringJoiner sj, Node<T> h) {
        if (h == null) return;
        toString(sj, h.venstre);
        sj.add(h.verdi.toString());
        toString(sj, h.høyre);
    }

    public abstract boolean leggInnIterativ(T verdi);
    public abstract boolean leggInnRekursiv(T verdi);
}

class RSTreNedenfraOpp<T> extends RSTre<T> {
    // Legger inn ved å gå nedenfra og opp.

    public RSTreNedenfraOpp(Comparator<? super T> sammenlikner) {super(sammenlikner);}

    public static <T extends Comparable<? super T>> RSTre<T> rstre() {
        return new RSTreNedenfraOpp<>(Comparator.naturalOrder());
    }

    @Override
    public boolean leggInnIterativ(T verdi) {
        // Legger inn i rotnoden dersom treet er tomt.
        if (tom()) {
            rot = new Node<>(verdi);
            rot.farge = SORT;
            antall++;
            return true;
        }

        // Går gjennom treet og finner riktig plassering for den nye noden
        // Lagrer nodene i en stabel, så vi kan gå baklengs når vi rydder opp
        Node<T> p = rot;
        Deque<Node<T>> stabel = new ArrayDeque<>();
        int cmp = 0;
        while (p != null) {
            stabel.push(p);
            cmp = sammenlikner.compare(verdi, p.verdi);
            if (cmp < 0) p = p.venstre;
            else if (cmp > 0) p = p.høyre;
            else return false; // Ulovlig med duplikater
        }

        p = new Node<>(verdi); // Funnet riktig plassering, lag ny node
        Node<T> f = stabel.pop(); // f er forelder til p, oppdater riktig venstre/høyrepeker
        if (cmp < 0) f.venstre = p;
        else f.høyre = p;
        antall++;

        if (!erRød(f)) return true; // Regel 2: Sort forelder. Vi er ferdig.

        Node<T> b = stabel.pop(); // Besteforelder til p
        while (erRød(b.venstre) && erRød(b.høyre)) {
            // Rød forelder OG Rød tante. Regel 1 gjelder.
            // Vi utfører et fargeskift, og ser om besteforelder er lovlig plassert
            fargeskift(b);
            if (rot == b) { // om b er rota, gjør sort, og vi er ferdig.
                rot.farge = SORT;
                return true;
            }
            // hopp til neste mulige problem
            p = b; f = stabel.pop();
            if (!erRød(f)) return true; // Regel 2: Sort forelder. Vi er ferdig.
            b = stabel.pop(); // Vi gjør denne etter testen over, unngår at vi går forbi rota.
        }

        // Vi vet nå at vi har sort tante. Regel 3 eller 4 gjelder.
        if (f == b.venstre) {
            if (p == f.høyre) // b-f-p ligger mot venstre, med knekk
                b.venstre = venstreRoter(f); // Rotert bort knekk, b-f-p ligger nå på linje
            p = høyreRoter(b); // Trenger ikke lenger p-pekeren, bruk til å holde styr på nye besteforelder
        } else {
            if (p == f.venstre) // b-f-p ligger mot høyre, med knekk
                b.høyre = høyreRoter(f); // Rotert bort knekk, b-f-p ligger nå på linje
            p = venstreRoter(b); // Trenger ikke lenger p-peker, bruk til å holde styr på nye besteforelder
        }
        // Oppdater nye besteforelder til å være riktig iht oldeforelder
        if (rot == b)
            rot = p;
        else {
            Node<T> o = stabel.pop();
            if (b == o.venstre)
                o.venstre = p;
            else
                o.høyre = p;
        }
        return true;
    }

    public boolean leggInnRekursiv(T verdi) {
        int forrigeAntall = antall;
        rot = leggInnRekursiv(rot, verdi);
        rot.farge = SORT;
        return antall != forrigeAntall; // Må se om antallet har økt for å se om vi faktisk har lagt inn noe
    }

    private Node<T> leggInnRekursiv(Node<T> h, T verdi) {
        if (h == null) { // Om vi er på bunn, øk antall, og legg inn ny rød node
            antall++;
            return new Node<>(verdi);
        }

        int cmp = sammenlikner.compare(verdi, h.verdi);
        if (cmp < 0) h.venstre = leggInnRekursiv(h.venstre, verdi); // Gå mot venstre
        else if (cmp > 0) h.høyre = leggInnRekursiv(h.høyre, verdi); // Gå mot høyre
        else return h; // Ikke lov med duplikater, ingen endring

        // Idéen nå er å gå baklengs og rette opp i feil etter hvert som vi ser dem.
        // Men vi vet ikke hvordan vi skal rette feilen før vi kommer til besteforelderen
        // Så denne algoritmen er skrevet fra besteforelderens synspunkt
        // Det gjør beklageligvis at vi må sjekke _begge_ retninger, inkludert retninga vi ikke kom fra

        if (erRød(h.venstre) && erRød(h.venstre.høyre)) {
            // Knekk mot venstre, Regel 1 eller 4, avhengig av tantefarge
            if (erRød(h.høyre)) // Rød tante, Regel 1.
                fargeskift(h);
            else // Sort tante, Regel 4 (første halvdel, fikser knekk)
                h.venstre = venstreRoter(h.venstre);
        } else if (erRød(h.høyre) && erRød(h.høyre.venstre)) {
            // Knekk mot høyre, Regel 1 eller 4, avhengig av tantefarge
            if (erRød(h.venstre)) // Rød tante, Regel 1.
                fargeskift(h);
            else // Sort tante, Regel 4 (første halvdel, fikser knekk)
                h.høyre = høyreRoter(h.høyre);
        }

        // Alle knekker er løst, men kan fremdeles ha røde noder på linje (Regel 3)
        if (erRød(h.venstre) && erRød(h.venstre.venstre)) {
            // Rett linje mot venstre, Regel 1 eller 3, avhengig av tantefarge
            if (erRød(h.høyre)) // Rød tante, Regel 1.
                fargeskift(h);
            else // Sort tante, Regel 3
                h = høyreRoter(h);
        } else if (erRød(h.høyre) && erRød(h.høyre.høyre)) {
            // Rett linje mot høyre, Regel 1 eller 3, avhengig av tantefarge
            if (erRød(h.venstre)) // Rød tante, Regel 1
                fargeskift(h);
            else // Sort tante, Regel 3
                h = venstreRoter(h);
        }
        return h;
    }
}

class VLRSTreNedenfraOpp<T> extends RSTre<T> {
    // Venstrelent tre, legger inn ved å gå nedenfra og opp

    public VLRSTreNedenfraOpp(Comparator<? super T> sammenlikner) {super(sammenlikner);}
    public static <T extends Comparable<? super T>> RSTre<T> rstre() {
        return new VLRSTreNedenfraOpp<>(Comparator.naturalOrder());
    }

    @Override
    public boolean leggInnIterativ(T verdi) {
        // Legger inn i rotnoden dersom treet er tomt
        if (tom()) {
            rot = new Node<>(verdi);
            rot.farge = SORT;
            antall++;
            return true;
        }

        // Går gjennom treet og finner riktig plassering for den nye noden
        // Lagrer nodene i en stabel, så vi kan gå baklengs når vi rydder opp
        Node<T> p = rot;
        Deque<Node<T>> stabel = new ArrayDeque<>();
        int cmp = 0;
        while (p != null) {
            stabel.push(p);
            cmp = sammenlikner.compare(verdi, p.verdi);
            if (cmp < 0) p = p.venstre;
            else if (cmp > 0) p = p.høyre;
            else return false; // Ulovlig med duplikater
        }

        p = new Node<>(verdi); // Funnet riktig plassering, lag ny node
        Node<T> b, f = stabel.pop(); // f er forelder til p, oppdater riktig venstre/høyrepeker
        if (cmp < 0) f.venstre = p;
        else f.høyre = p;
        antall++;

        if (!erRød(f)) { // Modifisert Regel 2: Sort forelder, men kan være høyrelent
            if (!erRød(f.venstre) && erRød(f.høyre)) {
                // Høyrelent, må utføre en rotasjon
                p = venstreRoter(f);
                // Oppdater riktig peker etter rotasjon
                if (rot == f)
                    rot = p;
                else {
                    b = stabel.pop();
                    if (f == b.venstre)
                        b.venstre = p;
                    else
                        b.høyre = p;
                }
            }
            return true; // Fiksa høyrelenthet, sort forelder. Vi er ferdig.
        }

        b = stabel.pop(); // Besteforelder til p
        while (erRød(b.venstre) && erRød(b.høyre)) {
            // Rød forelder OG Rød tante. Regel 1 gjelder.
            // Vi utfører et fargeskift, og sjekker om besteforelder er lovlig plassert
            // Må også her muligens fikse høyrelenthet
            fargeskift(b);
            if (!erRød(f.venstre) && erRød(f.høyre)) { // Høyrelent, roter for å fikse
                p = venstreRoter(f); // trenger ikke p-node lenger, bruk til å holde styr på ny forelder
                if (f == b.venstre) b.venstre = p;
                else b.høyre = p;
            }
            if (rot == b) { // om b er rota, gjør sort, og vi er ferdig.
                rot.farge = SORT;
                return true;
            }
            // hopp mot neste mulige problem
            f = stabel.pop();

            if (!erRød(f)) { // Modifisert Regel 2: Sort forelder, men kan være høyrelent
                if (!erRød(f.venstre) && erRød(f.høyre)) {
                    // Høyrelent, må utføre en rotasjon
                    p = venstreRoter(f);
                    // Oppdater riktig peker etter rotasjon
                    if (rot == f)
                        rot = p;
                    else {
                        b = stabel.pop();
                        if (f == b.venstre)
                            b.venstre = p;
                        else
                            b.høyre = p;
                    }
                }
                return true; // Fiksa høyrelenthet, sort forelder. Vi er ferdig.
            }
            b = stabel.pop();
        }

        // Vi vet nå at vi har sort tante. Regel 3 eller 4 gjelder.
        // Kun to muligheter. Må uansett gjøre en høyrerotasjon. Må vi gjøre en venstrerotasjon og?
        if (erRød(b.venstre.høyre))
            b.venstre = venstreRoter(b.venstre);
        p = høyreRoter(b);
        // Oppdater nye besteforelder til å være riktig iht oldeforelder
        if (rot == b)
            rot = p;
        else {
            Node<T> o = stabel.pop();
            if (b == o.venstre)
                o.venstre = p;
            else
                o.høyre = p;
        }
        return true;
    }

    @Override
    public boolean leggInnRekursiv(T verdi) {
        int forrigeAntall = antall;
        rot = leggInnRekursiv(rot, verdi);
        rot.farge = SORT;
        return antall != forrigeAntall; // Må se om antallet har økt for å se om vi faktisk har lagt inn noe
    }

    private Node<T> leggInnRekursiv(Node<T> h, T verdi) {
        if (h == null) { // Om vi er på bunn, øk antall, og legg inn ny rød node
            antall++;
            return new Node<>(verdi);
        }

        int cmp = sammenlikner.compare(verdi, h.verdi);
        if (cmp < 0) h.venstre = leggInnRekursiv(h.venstre, verdi); // Gå mot venstre
        else if (cmp > 0) h.høyre = leggInnRekursiv(h.høyre, verdi); // Gå mot høyre
        else return h; // Ikke lov med duplikater, ingen endring

        // Idéen er igjen å gå baklengs og rette opp i feil etter hvert som vi ser dem
        // Vi har introduert en ny type feil, høyrelenthet, som vi også må fikse
        // Denne feilen kan vi se fra forelderens perspektiv, så vil fikses derfra
        // Resten av feilene må fikses fra besteforelders perspektiv

        // Denne testen utfører "triple duty", siden den _også_ tar seg av venstrerotasjonen
        // som trengs om vi har en knekk, _og_ eventuelle gale rotasjoner i en fremtidig 4-node
        if (!erRød(h.venstre) && erRød(h.høyre)) h = venstreRoter(h);

        if (erRød(h.venstre) && erRød(h.venstre.venstre)) { // Enten Regel 1 eller 3, avhengig av tantefarge
            if (erRød(h.høyre)) // Regel 1
                fargeskift(h);
            else
                h = høyreRoter(h); // Regel 3
        } else if (erRød(h.høyre) && erRød(h.høyre.venstre)) // Regel 1
            fargeskift(h);
        return h;
    }
}

class VLRSTreOvenfraNed<T> extends RSTre<T> {
    // Venstrelent tre, legger inn ved å gå ovenfra og ned

    public VLRSTreOvenfraNed(Comparator<? super T> sammenlikner) {super(sammenlikner);}
    public static <T extends Comparable<? super T>> RSTre<T> rstre() {
        return new VLRSTreOvenfraNed<>(Comparator.naturalOrder());
    }

    @Override
    public boolean leggInnIterativ(T verdi) {
        throw new UnsupportedOperationException("Ikke giddi implementere dette.");
    }

    @Override
    public boolean leggInnRekursiv(T verdi) {
        int forrigeAntall = antall;
        rot = leggInnRekursiv(rot, verdi);
        rot.farge = SORT;
        return antall != forrigeAntall;
    }

    private Node<T> leggInnRekursiv(Node<T> h, T verdi) {
        if (h == null) { // Om vi er på bunn, øk antall, og legg inn ny rød node
            antall++;
            return new Node<>(verdi);
        }

        // På vei ned, dytt opp midterste verdi i en 4-node
        if (erRød(h.venstre) && erRød(h.høyre)) fargeskift(h);

        int cmp = sammenlikner.compare(verdi, h.verdi);
        if (cmp < 0) h.venstre = leggInnRekursiv(h.venstre, verdi); // Gå mot venstre
        else if (cmp > 0) h.høyre = leggInnRekursiv(h.høyre, verdi); // Gå mot høyre
        else return h; // Ikke lov med duplikater.
        // Merk at _treet_ kan bli endret selv om vi ikke legger inn noe. Hva synes vi om det?

        if (!erRød(h.venstre) && erRød(h.høyre)) h = venstreRoter(h); // Ikke venstrelent, fiks
        if (erRød(h.venstre) && erRød(h.venstre.venstre)) h = høyreRoter(h); // to røde på rad, fiks

        return h;
    }
}

class BokRSTre<T> extends RSTre<T> {
    // Wrapper for bokas tre, kun så java tror de er av samme type
    RSBinTre<T> rsbintre;
    public BokRSTre(Comparator<? super T> sammenlikner) {
        super(sammenlikner);
        rsbintre = new RSBinTre<>(sammenlikner);
    }

    public static <T extends Comparable<? super T>> RSTre<T> rstre() {
        return new BokRSTre<>(Comparator.naturalOrder());
    }

    @Override
    public boolean leggInnIterativ(T verdi) {
        return rsbintre.leggInn(verdi);
    }

    @Override
    public boolean leggInnRekursiv(T verdi) {
        throw new UnsupportedOperationException("Bokas rødsort-tre har ingen rekursiv innlegging implementert.");
    }

    @Override
    public int høyde() {
        return rsbintre.høyde();
    }
}

// Bokas implementasjon. Direkte kopiert. Kun bytta ut "Stakk" med "Stabel. Ikke giddi kopiert inn "Beholder"
class RSBinTre<T>
{
    private static final boolean SVART = true;
    private static final boolean RØD   = false;

    private static final class Node<T>    // en indre nodeklasse
    {
        private T verdi;             // nodens verdi
        private Node<T> venstre;     // peker til venstre barn
        private Node<T> høyre;       // peker til høyre barn
        private boolean farge;       // RØD eller SVART

        // konstruktør
        private Node(T verdi, Node<T> v, Node<T> h, boolean farge)
        {
            this.verdi = verdi;
            venstre = v; høyre = h;
            this.farge = farge;
        }

    } // slutt på class Node

    private final Node<T> NULL;            // en svart nullnode
    private Node<T> rot;                   // treets rot
    private int antall;                    // antall verdier
    private final Comparator<? super T> comp;    // treets komparator

    public RSBinTre(Comparator<? super T> comp)  // konstruktør
    {
        rot = NULL = new Node<>(null,null,null,SVART);
        this.comp = comp;
    }

    // en konstruksjonsmetode
    public static <T extends Comparable<? super T>> RSBinTre<T> lagTre()
    {
        return new RSBinTre<>(Comparator.<T>naturalOrder());
    }

    public int antall()
    {
        return antall;
    }

    public boolean tom()
    {
        return antall == 0;
    }

    public boolean leggInn(T verdi)
    {
        if (rot == NULL)         // treet er tomt
        {
            rot = new Node<>(verdi,NULL,NULL,SVART);
            antall++;
            return true;
        }

        // bruke en stakk for å kunne gå oppover etterpå
        Deque<Node<T>> stabel = new ArrayDeque<>();

        Node<T> p = rot;   // hjelpevariabel
        int cmp = 0;       // hjelpevariabel

        while (p != NULL)
        {
            stabel.push(p);                   // legger p på stakken

            cmp = comp.compare(verdi,p.verdi);  // sammenligner

            if (cmp < 0) p = p.venstre;         // til venstre
            else if (cmp > 0) p = p.høyre;      // til høyre
            else return false;                  // duplikater ikke tillatt
        }

        Node<T> x = new Node<>(verdi,NULL,NULL,RØD);

        antall++;                             // en ny verdi i treet

        Node<T> f = stabel.pop();             // forelder til x

        if (cmp < 0) f.venstre = x;           // x blir venstre barn
        else f.høyre = x;                     // x blir høyre barn

        if (f.farge == SVART) return true;    // vellykket innlegging

        // Hva hvis f er RØD?

        Node<T> b = stabel.pop();    // b for besteforelder

        while (true)
        {
            Node<T> s = (f == b.venstre) ? b.høyre : b.venstre;

            if (s.farge == SVART)
            {
                b.farge = RØD;

                if (x == f.venstre)         // 1a) eller 2b)
                {
                    if (f == b.venstre)       // 1a)
                    {
                        p = EHR(b); f.farge = SVART;  // enkel høyrerotasjon + fargeskifte
                    }
                    else  // x == f.høyre     // 2b)
                    {
                        p = DVR(b); x.farge = SVART;  // dobbel venstrerotasjon + fargeskifte
                    }
                }
                else // x == f.høyre        // 1b) eller 2a)
                {
                    if (f == b.venstre)       // 2a)
                    {
                        p = DHR(b); x.farge = SVART;  // dobbel høyrerotasjon + fargeskifte
                    }
                    else  // f == b.høyre     // 1b)
                    {
                        p = EVR(b); f.farge = SVART;  // enkel venstrerotasjon + fargeskifte
                    }
                }

                if (b == rot) rot = p;  // Hvis b var rotnoden, så må roten oppdateres
                else
                {
                    Node<T> q = stabel.pop();
                    if (b == q.venstre) q.venstre = p;
                    else q.høyre = p;
                }

                return true;  // to røde noder på rad er nå avverget
            }
            else  // s.farge == RØD
            {
                f.farge = s.farge = SVART;          // f og s blir svarte

                if (b == rot) return true;          // vi stopper

                b.farge = RØD;                      // b blir RØD

                // er forelder til b (dvs. oldeforelder til x) rød?

                Node<T> o = stabel.pop();           // oldeforelder til x
                if (o.farge == SVART) return true;  // vi stopper

                // nå har den røde node b en rød forelder
                // vi omdøper x, f og b og fortsetter oppover

                x = b;                  // ny x
                f = o;                  // ny f
                b = stabel.pop();       // ny b

            } // else

        } // while

    } // leggInn

    public boolean inneholder(T verdi)
    {
        for (Node<T> p = rot; p != NULL; )
        {
            int cmp = comp.compare(verdi,p.verdi);
            if (cmp > 0) p = p.høyre;
            else if (cmp < 0) p = p.venstre;
            else return true;
        }
        return false;  // ikke funnet
    }

    public boolean fjern(T verdi)
            throws UnsupportedOperationException
    {
        // ikke implementert
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString()                   // hører til SBinTre
    {
        StringBuilder s = new StringBuilder();   // StringBuilder
        s.append('[');                           // starter med [
        if (!tom()) toString(rot,s);             // den rekursive metoden
        s.append(']');                           // avslutter med ]
        return s.toString();                     // returnerer
    }

    private void toString(Node<T> p, StringBuilder s)
    {
        if (p.venstre != NULL)               // p har et venstre subtre
        {
            toString(p.venstre, s);                // komma og mellomrom etter
            s.append(',').append(' ');             // den siste i det venstre
        }                                        // subtreet til p

        s.append(p.verdi);                       // verdien i p

        if (p.høyre != NULL)                 // p har et høyre subtre
        {
            s.append(',').append(' ');             // komma og mellomrom etter
            toString(p.høyre, s);                  // p siden p ikke er den
        }                                        // siste noden i inorden
    }

    public int høyde()
    {
        return høyde(rot);
    }

    private int høyde(Node<T> p)
    {
        if (p == NULL) return -1;
        return 1 + Math.max(høyde(p.venstre), høyde(p.høyre));
    }

    public void nullstill()
    {
        rot = NULL;
        antall = 0;
    }

    public Iterator<T> iterator()
    {
        return new InordenIterator();
    }

    private final class InordenIterator implements Iterator<T>
    {
        private final Deque<Node<T>> s;
        private Node<T> p;

        private InordenIterator()
        {
            s = new ArrayDeque<>();
            if (rot == NULL) p = NULL;     // tomt tre
            else p = først(s,rot);
        }

        // den første i inorden i det treet som har p som rot
        private Node<T> først(Deque<Node<T>> s, Node<T> p)
        {
            while (p.venstre != NULL)
            {
                s.addFirst(p);
                p = p.venstre;
            }
            return p;
        }

        @Override
        public boolean hasNext()
        {
            return p != NULL;
        }

        @Override
        public void remove()
        {
            // ikke implementert
            throw new UnsupportedOperationException();
        }

        @Override
        public T next()  // neste er med hensyn på inorden
        {
            if (p == NULL)
                throw new NoSuchElementException();

            T verdi = p.verdi;

            if (p.høyre != NULL) p = først(s,p.høyre);
            else p = s.isEmpty() ? NULL : s.removeFirst();
            return verdi;
        }

    } // slutt på class InordenIterator

    //////////////////////////////////
    /// Private hjelpemetoder ////////
    //////////////////////////////////

    private static <T> Node<T> EHR(Node<T> p)  // Enkel høyrerotasjon
    {
        Node<T> q = p.venstre;

        p.venstre = q.høyre;
        q.høyre = p;
        return q;
    }

    private static <T> Node<T> EVR(Node<T> p)  // Enkel venstrerotasjon
    {
        Node<T> q = p.høyre;

        p.høyre = q.venstre;
        q.venstre = p;
        return q;
    }

    private static <T> Node<T> DHR(Node<T> p)  // Dobbel høyrerotasjon
    {
        Node<T> q = p.venstre;
        Node<T> r = q.høyre;

        q.høyre = r.venstre;
        r.venstre = q;
        p.venstre = r.høyre;
        r.høyre = p;
        return r;
    }

    private static <T> Node<T> DVR(Node<T> p)  // Dobbel venstrerotasjon
    {
        Node<T> q = p.høyre;
        Node<T> r = q.venstre;

        q.venstre = r.høyre;
        r.høyre = q;
        p.høyre = r.venstre;
        r.venstre = p;
        return r;
    }

} // class RSTre
