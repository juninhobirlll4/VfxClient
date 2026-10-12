# Identidade visual do VfxClient

Todos os assets aqui são **nossos**. Nada de textura, ícone ou fonte de outro client entra no repo.

## Paleta

| Token | Hex | Uso |
|---|---|---|
| `primary-deep` | `#5B16D0` | Início do degradê da marca, sombras |
| `primary` | `#7B2FF7` | Cor principal (botões, destaques) |
| `primary-light` | `#B48CFF` | Meio do degradê |
| `white` | `#FFFFFF` | Fim do degradê, texto sobre roxo |
| `bg` | `#120C1F` | Fundo do launcher |
| `surface` | `#1C1430` | Painéis |
| `muted` | `#A99CC4` | Texto secundário |

Degradê oficial: **roxo → branco** (`#7B2FF7` → `#B48CFF` → `#FFFFFF`), da esquerda para a direita.

## Fluxo SVG → PNG

1. Edite os arquivos em `svg/` (fonte da verdade, versionados).
2. Rode `python branding/render_icons.py` (instruções no topo do script).
3. O script gera `generated/` e copia os ícones para o mod (`assets/vfx/`) e para o launcher.
4. Faça commit dos SVGs **e** dos PNGs gerados.

## Status

`logo.svg` e `wordmark.svg` são **placeholders v0**: funcionam, mas devem ser trocados pela arte final.
Basta manter os mesmos nomes de arquivo e rodar o script de novo.
