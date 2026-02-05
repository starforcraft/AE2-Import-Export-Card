---
navigation:
  title: "Add-on: Cartão de Import./Export. do AE2"
  icon: ae2importexportcard:export_card
  position: 150
categories:
  - tools
item_ids:
- ae2importexportcard:export_card
- ae2importexportcard:import_card
---

# Cartão de Import./Export. do AE2

<Row>
  <ItemImage id="ae2importexportcard:export_card" scale="2" />

  <ItemImage id="ae2importexportcard:import_card" scale="2" />
</Row>

Cartões de Importação e Exportação permitem que você importe/exporte itens do seu inventário

## Cartão de Importação

<ItemImage id="ae2importexportcard:import_card" scale="2" />

O cartão de importação pega itens em slots específicos do seu inventário e os despeja em seu sistema ME.

![Cartão de Importação](diagrams/import_card.png)

Clicar nos slots cria uma marca de seleção. Qualquer item no slot que tiver uma marca de seleção será importado para 
o seu sistema ME. Arraste itens do seu inventário para o topo para alterar o filtro.

### Melhorias

O Cartão de Importação suporta as seguintes [melhorias](items-blocks-machines/upgrade_cards.md):

*   <ItemLink id="fuzzy_card" /> filtra pelo nível de dano e/ou ignora NBT do item
*   <ItemLink id="inverter_card" /> altera o filtro de uma lista branca para uma lista negra

### Receita

<RecipeFor id="ae2importexportcard:import_card" />

## Cartão de Exportação

<ItemImage id="ae2importexportcard:export_card" scale="2" />

O cartão de exportação funciona exatamente da mesma maneira, mas puxará itens do seu sistema ME para o seu inventário.

![Cartão de Exportação](diagrams/export_card.png)

Para especificar quais itens, arraste o item do inventário para um dos slots na parte superior e clique em um 
slot no seu inventário para alterá-lo para o número desejado. Clicar com o botão direito limpa de volta para X.

### Melhorias

O Cartão de Exportação suporta as seguintes [melhorias](items-blocks-machines/upgrade_cards.md):

*   <ItemLink id="fuzzy_card" /> filtra pelo nível de dano e/ou ignora NBT do item
*   <ItemLink id="speed_card" /> melhora a velocidade de transferência de 1 para uma pilha inteira de itens
*   <ItemLink id="crafting_card" /> solicita e fabrica automaticamente itens que não estão disponíveis no momento

### Receita

<RecipeFor id="ae2importexportcard:export_card" />