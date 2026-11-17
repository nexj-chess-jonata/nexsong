package br.com.nexj.nexsong.classify

/**
 * Base local de artistas conhecidos por gênero (id do gênero -> nomes). Funciona 100% offline.
 * Para ampliar, basta acrescentar nomes nas listas abaixo.
 */
object ArtistDictionary {

    private val samba = listOf(
        "Beth Carvalho", "Zeca Pagodinho", "Martinho da Vila", "Cartola", "Paulinho da Viola",
        "Clara Nunes", "Alcione", "Jorge Aragão", "Fundo de Quintal", "Arlindo Cruz",
        "Dudu Nobre", "Dona Ivone Lara", "Elza Soares", "Noel Rosa", "Adoniran Barbosa",
        "Nelson Cavaquinho", "Os Originais do Samba", "Almir Guineto", "Bezerra da Silva",
        "Leci Brandão", "Diogo Nogueira", "Monarco", "Wilson das Neves", "Xande de Pilares",
        "Demônios da Garoa", "João Nogueira", "Roberto Ribeiro", "Candeia", "Teresa Cristina",
        "Maria Rita Samba", "Benito di Paula", "Agepê", "Neguinho da Beija-Flor"
    )

    private val pagode = listOf(
        "Sorriso Maroto", "Exaltasamba", "Pixote", "Revelação", "Grupo Revelação", "Raça Negra",
        "Thiaguinho", "Menos é Mais", "Molejo", "Negritude Júnior", "Mumuzinho", "Belo",
        "Péricles", "Art Popular", "Katinguelê", "Soweto", "Só Pra Contrariar",
        "Turma do Pagode", "Ferrugem", "Dilsinho", "Jeito Moleque", "Os Travessos",
        "Harmonia do Samba", "Imaginasamba", "Bom Gosto", "Swing e Simpatia", "Di Propósito",
        "Vou pro Sereno", "Grupo Raça", "Karametade", "Sem Compromisso", "Alexandre Pires"
    )

    private val sertanejo = listOf(
        "Chitãozinho & Xororó", "Leandro & Leonardo", "Zezé Di Camargo & Luciano",
        "Zezé Di Camargo", "Bruno & Marrone", "Jorge & Mateus", "Gusttavo Lima",
        "Henrique & Juliano", "Marília Mendonça", "Maiara & Maraisa", "Luan Santana",
        "Michel Teló", "Cristiano Araújo", "Fernando & Sorocaba", "Victor & Leo",
        "João Paulo & Daniel", "Milionário & José Rico", "Tonico & Tinoco", "Sérgio Reis",
        "Rionegro & Solimões", "Edson & Hudson", "Matheus & Kauan", "Zé Neto & Cristiano",
        "Simone Mendes", "Simone & Simaria", "Ana Castela", "Lauana Prado",
        "Israel & Rodolffo", "Hugo & Guilherme", "Pedro Paulo & Alex", "Gian & Giovani",
        "Cezar & Paulinho", "Chrystian & Ralf", "Rick & Renner", "Bruno & Barretto",
        "Day & Lara", "Eduardo Costa", "Paula Fernandes", "Humberto & Ronaldo",
        "Trio Parada Dura", "Inezita Barroso", "Almir Sater", "Renato Teixeira",
        "Pena Branca & Xavantinho", "Tião Carreiro & Pardinho", "Liu & Léu",
        "Léo Canhoto & Robertinho", "João Bosco & Vinícius", "Guilherme & Santiago",
        "Thaeme & Thiago", "Fiduma & Jeca", "Felipe Araújo", "Murilo Huff", "Zé Felipe",
        "Jads & Jadson", "Hugo Pena & Gabriel", "Cleber & Cauan", "Lucas Lucco",
        "Diego & Victor Hugo", "Luíza & Maurílio", "Gustavo Mioto", "Naiara Azevedo",
        "Roberta Miranda", "Leonardo"
    )

    private val rock = listOf(
        "Legião Urbana", "Os Paralamas do Sucesso", "Paralamas do Sucesso", "Titãs",
        "Barão Vermelho", "Cazuza", "Raul Seixas", "Rita Lee", "Capital Inicial", "Skank",
        "Engenheiros do Hawaii", "Charlie Brown Jr.", "Nenhum de Nós", "Raimundos",
        "Sepultura", "Pitty", "Detonautas", "Lobão", "Ira!", "Plebe Rude", "Kid Abelha",
        "Ultraje a Rigor", "Biquini Cavadão", "Pato Fu", "Los Hermanos", "CPM 22",
        "NX Zero", "Fresno", "Jota Quest", "Dr. Sin", "Angra", "Mutantes",
        "Camisa de Vênus", "RPM", "Léo Jaime", "Cássia Eller",
        "Queen", "The Beatles", "Led Zeppelin", "Pink Floyd", "The Rolling Stones", "AC/DC",
        "Nirvana", "Metallica", "Guns N' Roses", "U2", "Deep Purple", "Black Sabbath",
        "Iron Maiden", "Foo Fighters", "Red Hot Chili Peppers", "Linkin Park", "Green Day",
        "Bon Jovi", "Scorpions", "Aerosmith", "Kiss", "Ozzy Osbourne", "The Doors",
        "Eagles", "Creedence Clearwater Revival", "Def Leppard", "Pearl Jam",
        "System of a Down", "Oasis", "Radiohead", "The Cure", "Van Halen", "Journey",
        "Foreigner", "Dire Straits", "Soundgarden", "Alice in Chains", "Megadeth", "Slayer",
        "Pantera", "Ramones", "The Clash", "Sex Pistols", "Lynyrd Skynyrd", "ZZ Top",
        "Whitesnake", "Mötley Crüe", "Skid Row", "Judas Priest", "Motörhead", "Rammstein",
        "Evanescence", "Nickelback", "Blink-182", "The Offspring", "Weezer",
        "Arctic Monkeys", "The Strokes", "Muse", "Kings of Leon", "Elvis Presley"
    )

    private val forro = listOf(
        "Luiz Gonzaga", "Dominguinhos", "Trio Nordestino", "Falamansa", "Calcinha Preta",
        "Aviões do Forró", "Mastruz com Leite", "Flávio José", "Santanna o Cantador",
        "Wesley Safadão", "Xand Avião", "Garota Safada", "Solteirões do Forró", "Magníficos",
        "João Gomes", "Mari Fernandez", "Nattanzinho Lima", "Nattan", "Vitor Fernandes",
        "Jackson do Pandeiro", "Marinês", "Elba Ramalho", "Petrúcio Amorim", "Limão com Mel",
        "Cavaleiros do Forró", "Taty Girl", "Os Três do Nordeste", "Sivuca", "Genival Lacerda",
        "Frank Aguiar", "Barões da Pisadinha", "Os Barões da Pisadinha", "Zé Vaqueiro",
        "Ávine Vinny", "Waldonys", "Joquinha Gonzaga", "Forró Pegado", "Tarcísio do Acordeon",
        "Iguinho e Lulinha", "Desejo de Menina", "Banda Magníficos", "Dorgival Dantas"
    )

    private val mpb = listOf(
        "Caetano Veloso", "Gilberto Gil", "Chico Buarque", "Milton Nascimento", "Djavan",
        "Maria Bethânia", "Gal Costa", "Elis Regina", "Tom Jobim", "Vinicius de Moraes",
        "João Gilberto", "Marisa Monte", "Jorge Ben Jor", "Jorge Ben", "Tim Maia",
        "Roberto Carlos", "Ivan Lins", "Zé Ramalho", "Alceu Valença", "Belchior", "Fagner",
        "Nando Reis", "Ana Carolina", "Seu Jorge", "Vanessa da Mata", "Maria Rita", "Lenine",
        "Zeca Baleiro", "Adriana Calcanhotto", "Tribalistas", "Arnaldo Antunes",
        "Carlinhos Brown", "Geraldo Azevedo", "Oswaldo Montenegro", "Gonzaguinha",
        "Ney Matogrosso", "Secos & Molhados", "Novos Baianos", "Tiago Iorc", "Anavitória",
        "Vitor Kley", "Melim", "Maria Gadú", "Toquinho", "Emílio Santiago", "Luiz Melodia",
        "Guilherme Arantes", "Flávio Venturini", "14 Bis", "Roupa Nova", "Erasmo Carlos",
        "Nara Leão", "Beto Guedes", "Lulu Santos", "Paulinho Moska", "Jão", "Liniker"
    )

    private val funk = listOf(
        "Anitta", "Ludmilla", "Kevinho", "Claudinho & Buchecha", "Dennis DJ", "Dennis",
        "Bonde do Tigrão", "Pedro Sampaio", "Valesca Popozuda", "Tati Quebra Barraco",
        "Mr. Catra", "Furacão 2000", "DJ Marlboro", "Pocah", "Lexa", "Jottapê",
        "MC Kevinho", "MC Livinho", "MC Hariel", "MC Ryan SP", "MC Poze do Rodo",
        "MC Marcinho", "MC Koringa", "MC Guimê", "MC Don Juan", "MC Pedrinho", "MC Kekel",
        "MC Fioti", "MC G15", "MC Zaac", "MC Daniel", "MC Davi", "MC Paiva", "MC Sapão",
        "MC Leozinho", "MC IG", "MC PH", "MC Rebecca", "MC Kevin", "MC Lan", "MC WM"
    )

    private val eletronica = listOf(
        "Alok", "Vintage Culture", "David Guetta", "Calvin Harris", "Avicii", "Tiësto",
        "Martin Garrix", "Daft Punk", "Skrillex", "Marshmello", "Kygo", "Armin van Buuren",
        "Deadmau5", "The Chainsmokers", "Swedish House Mafia", "Dubdogz", "KVSH",
        "Cat Dealers", "Bhaskar", "Zedd", "Diplo", "Major Lazer", "DJ Snake", "Robin Schulz",
        "Fatboy Slim", "The Prodigy", "The Chemical Brothers", "Hardwell", "Steve Aoki",
        "Afrojack", "Illusionize", "Meduza", "Purple Disco Machine", "Bob Sinclar",
        "Benny Benassi", "Gigi D'Agostino", "Alan Walker", "Lost Frequencies", "Felguk"
    )

    private val hiphop = listOf(
        "Racionais MC's", "Emicida", "Criolo", "Sabotage", "Marcelo D2", "Projota", "Djonga",
        "Matuê", "Filipe Ret", "Xamã", "L7nnon", "Orochi", "Baco Exu do Blues", "Rashid",
        "Hungria Hip Hop", "Hungria", "Tribo da Periferia", "Costa Gold", "Haikaiss", "1Kilo",
        "Poesia Acústica", "Veigh", "Kayblack", "MC Cabelinho", "Rincon Sapiência",
        "Gabriel o Pensador", "Planet Hemp", "Eminem", "Tupac", "2Pac",
        "The Notorious B.I.G.", "Jay-Z", "Kanye West", "Drake", "Kendrick Lamar",
        "Snoop Dogg", "Dr. Dre", "50 Cent", "Travis Scott", "Post Malone", "Nicki Minaj",
        "Cardi B", "Lil Wayne", "J. Cole", "Wu-Tang Clan", "Ice Cube", "N.W.A"
    )

    private val gospel = listOf(
        "Aline Barros", "Fernandinho", "Gabriela Rocha", "Anderson Freire", "Bruna Karla",
        "Cassiane", "Damares", "Thalles Roberto", "Diante do Trono", "Ana Paula Valadão",
        "André Valadão", "Oficina G3", "Fernanda Brum", "Eyshila", "Isadora Pompeo",
        "Preto no Branco", "Kleber Lucas", "Regis Danese", "Davi Sacer", "Trazendo a Arca",
        "Toque no Altar", "Ministério Zoe", "Casa Worship", "Hillsong", "Hillsong United",
        "Hillsong Worship", "Elevation Worship", "Bethel Music", "Padre Marcelo Rossi",
        "Padre Fábio de Melo", "Padre Zezinho", "Padre Reginaldo Manzotti",
        "Shirley Carvalhaes", "Mara Lima", "Voz da Verdade", "Renascer Praise",
        "Leonardo Gonçalves", "Paulo César Baruk", "Midian Lima", "Sarah Farias",
        "Valesca Mayssa", "Jessé Aguiar", "Eli Soares", "Marquinhos Gomes",
        "Rose Nascimento", "Lauriete", "Elaine de Jesus", "Nívea Soares", "Ludmila Ferber",
        "Heloisa Rosa", "Gabriel Guedes", "Julliany Souza", "Theo Rubia", "Fhop Music",
        "Colo de Deus", "Rosa de Saron", "Anjos de Resgate", "Sarah Beatriz", "Stella Laura"
    )

    private val internacional = listOf(
        "Michael Jackson", "Madonna", "Beyoncé", "Rihanna", "Taylor Swift", "Ed Sheeran",
        "Adele", "Bruno Mars", "Lady Gaga", "Justin Bieber", "Ariana Grande", "Katy Perry",
        "Shakira", "Dua Lipa", "The Weeknd", "Billie Eilish", "Coldplay", "Maroon 5",
        "Imagine Dragons", "ABBA", "Bee Gees", "Elton John", "Whitney Houston",
        "Celine Dion", "Mariah Carey", "Britney Spears", "Backstreet Boys",
        "Justin Timberlake", "Harry Styles", "One Direction", "Sam Smith", "Shawn Mendes",
        "Selena Gomez", "Miley Cyrus", "Olivia Rodrigo", "Bad Bunny", "Luis Fonsi",
        "Enrique Iglesias", "Ricky Martin", "Stevie Wonder", "Lionel Richie", "Phil Collins",
        "George Michael", "Tina Turner", "Bob Marley", "Frank Sinatra", "Doja Cat",
        "Christina Aguilera", "Jennifer Lopez", "Black Eyed Peas", "Roxette", "A-ha"
    )

    /** Ordem importa: o primeiro gênero que contiver o artista vence. */
    val byGenre: List<Pair<String, List<String>>> = listOf(
        "gospel" to gospel,
        "forro" to forro,
        "pagode" to pagode,
        "samba" to samba,
        "sertanejo" to sertanejo,
        "funk" to funk,
        "hiphop" to hiphop,
        "eletronica" to eletronica,
        "mpb" to mpb,
        "rock" to rock,
        "internacional" to internacional
    )
}
