<template>
<div class="book-text" ref="text" @mouseup="showPopup">
  <slot>
    <div v-html="content"></div>
  </slot>
  <transition name="fade">
    <div class="book-text-popup" v-show="displayPopup" ref="popup">
      Quote:
      <a href="#" @click.prevent.stop="copyPlainQuote">Plain</a> &middot;
      <a href="#" @click.prevent.stop="copyDiscordQuote">Discord</a> &middot;
      <a href="#" @click.prevent.stop="copyWikiQuote">Wiki</a>
    </div>
  </transition>
</div>
</template>

<script>
export default {
  name: 'BookText',
  props: {
    bookTitle: String,
    chapterTitle: String,
    content: String,
  },
  data() {
    return {
      normalizationTimeout: null,
      displayPopup: false,
    };
  },
  mounted() {
    document.addEventListener('selectionchange', this.onSelectionChange);
  },
  destroyed() {
    document.removeEventListener('selectionchange', this.onSelectionChange);
  },
  methods: {
    onSelectionChange() {
      const selection = document.getSelection();
      const startNode = selection.anchorNode;
      const endNode = selection.focusNode;
      const startOffset = selection.anchorOffset;
      const endOffset = selection.focusOffset;

      const affectedBySelection = !selection.isCollapsed
        && startNode !== null && this.$refs.text.contains(startNode)
        && endNode !== null && this.$refs.text.contains(endNode);

      if (!affectedBySelection) {
        this.hidePopup();
        this.removeSelectionMarker('selection-start-marker');
        this.removeSelectionMarker('selection-end-marker');
      } else if (startNode !== endNode || startOffset > endOffset) {
        this.insertSelectionMarker('selection-start-marker', startNode, startOffset);
        this.insertSelectionMarker('selection-end-marker', endNode, endOffset);
      } else {
        this.insertSelectionMarker('selection-end-marker', endNode, endOffset);
        this.insertSelectionMarker('selection-start-marker', startNode, startOffset);
      }

      clearTimeout(this.normalizationTimeout);
      this.normalizationTimeout = setTimeout(() => {
        if (this.$refs.text) {
          this.$refs.text.normalize();
        }
      }, 100);
    },
    insertSelectionMarker(id, node, offset) {
      const marker = document.getElementById(id) || document.createElement('span');

      if (marker === node) {
        return;
      }

      marker.id = id;

      if (node.nodeType === Node.TEXT_NODE) {
        node.splitText(offset);
        node.parentNode.insertBefore(marker, node.nextSibling);
      } else if (node.nodeType === Node.ELEMENT_NODE) {
        if (offset === 0) {
          node.prepend(marker);
        } else if (offset > 0) {
          node.append(marker);
        }
      }
    },
    removeSelectionMarker(id) {
      const marker = document.getElementById(id);
      if (marker !== null && this.$refs.text.contains(marker)) {
        marker.remove();
      }
    },
    hidePopup() {
      this.displayPopup = false;
    },
    showPopup() {
      const { popup } = this.$refs;
      const startMarker = document.getElementById('selection-start-marker');
      const endMarker = document.getElementById('selection-end-marker');

      if (startMarker === null || !this.$refs.text.contains(startMarker)) {
        return;
      }

      let x = startMarker.offsetLeft;
      const y = Math.min(startMarker.offsetTop, endMarker.offsetTop);
      if (y < startMarker.offsetTop) {
        x = endMarker.offsetLeft;
      } else if (y === startMarker.offsetTop) {
        x = Math.min(startMarker.offsetLeft, endMarker.offsetLeft);
      }
      popup.style.left = `${x}px`;
      popup.style.top = `${y}px`;
      this.displayPopup = true;
    },
    copyPlainQuote() {
      this.copySelection('', '');
    },
    copyDiscordQuote() {
      this.copySelection(
        '```\n',
        `\n\n${this.bookTitle} - ${this.chapterTitle}\n\`\`\``,
      );
    },
    copyWikiQuote() {
      this.copySelection(
        '{{quote\n| ',
        `\n| ${this.bookTitle} - ${this.chapterTitle}\n}}`,
      );
    },
    copySelection(prefix, suffix) {
      const el = document.createElement('textarea');
      const selection = this.normalizeText(document.getSelection().toString().trim());
      el.value = `${prefix}${selection}${suffix}`;
      document.body.appendChild(el);
      el.select();

      document.execCommand('copy');

      document.body.removeChild(el);

      this.$notifications.success('The quote has been successfully copied to your clipboard!');
    },
    normalizeText(text) {
      return text
        .replace(/[\u00AB\u00BB\u201C\u201D\u201E]/g, '"') // Double quotes
        .replace(/\u0091\u0092\u2018\u2019\u201A\u2039\u203A\uFF07/, '\''); // Single quotes
    },
  },
};
</script>

<style lang="scss">
.book-text {
  font-family: 'Lora', serif;
  position: relative;

  &-popup {
    position: absolute;
    padding: 0.25rem;
    background: white;
    border: 1px solid #C8C8C8;
    border-radius: 3px;
    box-shadow: 0 0.25rem 1rem rgba(0, 0, 0, 0.1);
    transform: translateY(-100%);
    margin-top: -0.5rem;
    font-family: 'Nunito Sans', sans-serif;
    z-index: 2000;

    &:after {
      position: absolute;
      box-sizing: border-box;
      background: white;
      content: '';
      width: 0.5rem;
      height: 0.5rem;
      top: auto;
      bottom: 0;
      left: 0.7rem;
      margin-bottom: -0.25rem;
      transform-origin: 50%;
      transform: rotate(45deg);
      border: 1px solid #C8C8C8;
      border-top: none;
      border-left: none;
    }
  }

  h2 {
    margin: 0;
    padding: 0;
    font-family: 'Nunito Sans', sans-serif;
    font-size: 1.25rem;
    z-index: 1001;
    text-align: center;
  }

  p {
    margin: 0.5rem 0;

    &:first-child {
      margin-top: 0;
    }

    &:last-child {
      margin-bottom: 0;
    }
  }

  .h1 {
    text-align: center;
    font-size: 1.5rem;
  }

  .h2 {
    text-align: center;
    font-size: 1.25rem;
  }

  .h3 {
    text-align: center;
    font-size: 1.1rem;
  }

  .chapterText {
    text-align: justify;
    text-indent: 1rem;
  }

  img {
    display: inline-block;
    max-width: 100%;
  }

  .centeredImage {
    text-align: center;

    img {
      max-width: 80%;
    }
  }

  .epigraphText {
    width: 60%;
    margin-left: auto;
    margin-right: auto;
    text-align: justify;
  }

  .epigraphCitation {
    width: 60%;
    margin-left: auto;
    margin-right: auto;
    text-align: right;
  }

  * + .embed {
    margin-top: 1rem;
  }

  .embed {
    font-family: 'Nunito Sans', sans-serif;
    box-sizing: border-box;
    display: block;
    padding: 0 1.5rem;

    & + * {
      margin-top: 1rem;
    }

    & + .embed {
      margin-top: 0;
    }
  }

  samp {
    font-family: 'Nunito Sans', sans-serif;
    text-align: justify;
    display: block;
  }

  .italic {
    font-style: italic;
  }

  .bold {
    font-weight: bold;
  }

  .reset {
    font-style: normal;
    font-weight: normal;
  }

  .result-paragraph {
    background: aliceblue;
  }
}
</style>
