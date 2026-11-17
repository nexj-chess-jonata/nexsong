# NexSong 1.0

Gerenciador de áudio para Android que separa as músicas do celular por gênero, com player completo,
letras sincronizadas e detecção de duplicatas. A classificação funciona sem internet.

## O que o app faz

- **Biblioteca:** lê todas as músicas do aparelho, com busca, filtros (formato, duração, sem artista, com/sem letra),
  ordenação, abas por gênero e seleção múltipla (segure uma música).
- **Gêneros:** Samba, Sertanejo, Rock, Forró, Pagode, MPB, Funk, Eletrônica, Hip-Hop, Gospel, Internacional e Outros.
  Você pode criar, renomear e apagar gêneros.
- **Classificação (offline), nesta ordem:** sua correção, pasta já organizada, tag do arquivo, base local de artistas,
  nome da pasta e nome do artista (estes dois viram *sugestão*), o que o app aprendeu com você.
  Sugestões só vão para as pastas depois que você confirmar.
- **Aprendizado:** ao corrigir o gênero de algumas músicas, o app sugere o mesmo para as parecidas
  (mesmo artista, álbum ou pasta). Dá para esquecer o aprendizado.
- **Organizar Agora:** modo virtual (só no app), copiar para `Music/NexSong Cópias/<gênero>` ou mover para
  `Music/NexSong/<gênero>` (Android 11+, com confirmação e desfazer).
- **Player:** notificação e tela de bloqueio, fila editável, aleatório, repetir, equalizador.
- **Letras:** sincronizadas (karaokê) via LRCLIB, guardadas no aparelho para uso offline, com edição manual,
  ajuste de tempo e download em lote.
- **Editor:** título, artista, álbum, gênero e capa, em uma música ou em várias. As edições ficam só dentro do app
  (o arquivo de música não é alterado).
- **Duplicatas:** acha músicas repetidas, sugere qual manter e deixa você ocultar ou apagar as outras, sempre com confirmação.
- Tema escuro/claro (cores dinâmicas no Android 12+).

## Limites conhecidos

- A classificação **não analisa o áudio**: usa tags, base de artistas, pastas e suas correções.
- Reconhecimento de música por microfone (estilo Shazam) não faz parte desta versão.
- Mover arquivos e apagar duplicatas exigem Android 11 ou superior. No Android 10 use copiar/ocultar.
- Letras exigem internet apenas na primeira busca de cada música.

## Gerar o APK (GitHub Actions)

1. Crie um repositório no GitHub e envie todos os arquivos desta pasta (inclusive a pasta oculta `.github`).
2. Abra a aba **Actions** e rode **Gerar APK** (ou faça um push na branch `main`).
3. Ao terminar, abra a execução e baixe **NexSong-APK** em *Artifacts*. Dentro do zip está o `app-debug.apk`.
4. Instale no celular (permita instalar de fontes desconhecidas).

Requer Android 10 ou superior.
