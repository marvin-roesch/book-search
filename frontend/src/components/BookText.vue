<template>
<div class="book-text" ref="text">
  <slot>
    <div v-html="content" role="button"></div>
  </slot>
  <transition name="fade">
    <div class="book-text-popup" v-show="displayPopup" ref="popup">
      Quote:
      <a href="#" @click.prevent.stop="copyPlainQuote">Plain</a> &middot;
      <a href="#" @click.prevent.stop="copyDiscordQuote">Discord</a> &middot;
      <a href="#" @click.prevent.stop="copyWikiQuote">Wiki</a>
      <template v-if="citation !== null">
      &middot; <a href="#" @click.prevent.stop="copyWikiCitation">Wiki Citation</a>
      </template>
    </div>
  </transition>
</div>
</template>

<script>
import { copyText } from '@/utils';

export default {
  name: 'BookText',
  props: {
    bookTitle: String,
    chapterTitle: String,
    content: String,
    citation: String,
  },
  data() {
    return {
      normalizationTimeout: null,
      displayPopup: false,
    };
  },
  mounted() {
    document.addEventListener('selectionchange', this.onSelectionChange);
    document.addEventListener('mouseup', this.showPopup);
    document.addEventListener('touchstart', this.showPopup);
    document.addEventListener('touchend', this.showPopup);
    document.addEventListener('contextmenu', this.showPopup);
    this.$el.querySelectorAll('[data-footnote]').forEach((element) => {
      element.addEventListener('click', (event) => {
        const target = document.getElementById(new URL(element.href).hash.substring(1));
        if (target) {
          window.scrollTo({ top: target.offsetTop });
          target.classList.add('flash-paragraph');
          setTimeout(() => {
            target.classList.remove('flash-paragraph');
          }, 1000);
        }
        event.preventDefault();
      });
    });
    this.$el.querySelectorAll('[data-footnote-back]').forEach((element) => {
      element.addEventListener('click', (event) => {
        const target = document.getElementById(new URL(element.href).hash.substring(1));
        if (target) {
          window.scrollTo({ top: target.offsetTop });
          let parent = target;
          while (parent && parent.tagName.toLowerCase() !== 'p') {
            parent = parent.parentNode;
          }

          if (parent) {
            parent.classList.add('flash-paragraph');
            setTimeout(() => {
              parent.classList.remove('flash-paragraph');
            }, 1000);
          }
        }
        event.preventDefault();
      });
    });
  },
  destroyed() {
    document.removeEventListener('selectionchange', this.onSelectionChange);
    document.removeEventListener('mouseup', this.showPopup);
    document.removeEventListener('touchstart', this.showPopup);
    document.removeEventListener('touchend', this.showPopup);
    document.removeEventListener('contextmenu', this.showPopup);
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

      if (!startMarker || !this.$refs.text || !this.$refs.text.contains(startMarker)) {
        return;
      }

      let offsetElement = startMarker;
      if (startMarker.offsetTop > endMarker.offsetTop
        || (startMarker.offsetTop === endMarker.offsetTop
          && endMarker.offsetLeft <= startMarker.offsetLeft)) {
        offsetElement = endMarker;
      }
      let x = offsetElement.offsetLeft;

      const endOfLine = offsetElement.offsetParent.clientWidth === offsetElement.offsetLeft;
      let y = offsetElement.offsetTop;

      if (endOfLine) {
        x = 0;
        y += offsetElement.offsetHeight;
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
        '>>> ',
        '',
      );
    },
    copyWikiQuote() {
      this.copySelection(
        '{{quote\n',
        this.citation === null ? '\n}}' : `\n|${this.citation || ''}\n}}`,
        s => `|${s}`,
      );
    },
    copyWikiCitation() {
      copyText(this.citation);
      this.$notifications.success('The citation has been successfully copied to your clipboard!');
    },
    copySelection(prefix, suffix, transform = s => s) {
      const selection = this.normalizeText(document.getSelection().toString().trim());
      copyText(`${prefix}${transform(selection)}${suffix}`);
      this.$notifications.success('The quote has been successfully copied to your clipboard!');
    },
    normalizeText(text) {
      return text
        .replace(/[\u00AB\u00BB\u201C\u201D\u201E]/g, '"') // Double quotes
        .replace(/[\u0091\u0092\u2018\u2019\u201A\u2039\u203A\uFF07]/, '\''); // Single quotes
    },
  },
};
</script>

<style lang="scss">
.book-text {
  font-family: 'Lora', serif;
  position: relative;
  word-break: break-word;
  overflow: visible;

  &-popup {
    position: absolute;
    padding: 0.25rem;
    background: var(--section-bg);
    border: 1px solid rgba(0, 0, 0, 0.1);
    border-radius: 3px;
    box-shadow: 0 0.25rem 1rem rgba(0, 0, 0, 0.1);
    transform: translateY(-100%);
    margin-top: -0.5rem;
    font-family: 'Nunito Sans', sans-serif;
    z-index: 2000;
    max-width: 1000px;
    user-select: none;
    white-space: nowrap;

    &:after {
      position: absolute;
      box-sizing: border-box;
      background: var(--section-bg);
      content: '';
      width: 0.5rem;
      height: 0.5rem;
      top: auto;
      bottom: 0;
      left: 0.7rem;
      margin-bottom: -0.25rem;
      transform-origin: 50%;
      transform: rotate(45deg);
      border: 1px solid rgba(0, 0, 0, 0.1);
      border-top: none;
      border-left: none;
    }

    @media (max-width: $max-content-width) {
      position: fixed;
      left: 0 !important;
      right: 0 !important;
      top: auto !important;
      bottom: 0 !important;
      margin-top: 0;
      padding: 1rem;
      transform: none;
      border-radius: 0;
      border-left: none;
      border-right: none;
      border-bottom: none;

      &:after {
        display: none;
      }
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
    text-align: justify;
    text-indent: 1rem;
    margin: 0.5rem 0;

    &:first-child {
      margin-top: 0;
    }

    &:last-child {
      margin-bottom: 0;
    }
  }

  h1, .h1 {
    text-align: center;
    font-size: 1.5rem;
    text-indent: 0;
  }

  h2, .h2 {
    text-align: center;
    font-size: 1.25rem;
    text-indent: 0;
  }

  h3, .h3 {
    text-align: center;
    font-size: 1.1rem;
    text-indent: 0;
  }

  .chapterText {
    text-align: justify;
    text-indent: 1rem;
  }

  img {
    display: inline-block;
    max-width: 100%;
    margin: 0 auto;
  }

  .centeredImage, .fullWidthImage, .iconImage {
    text-align: center;
    text-indent: 0;
  }

  .centeredImage img {
    max-width: 80%;
  }

  .fullWidthImage img {
    max-width: 100%;
  }

  .iconImage img {
    max-width: 20%;
  }

  p.epigraphText {
    width: 60%;
    margin-left: auto;
    margin-right: auto;
    text-align: justify;
    text-indent: 0;
  }

  .epigraphCitation {
    width: 60%;
    margin-left: auto;
    margin-right: auto;
    text-align: right;
    text-indent: 0;
  }

  * + .embed {
    margin-top: 1rem;
  }

  .embed {
    font-family: 'Nunito Sans', sans-serif;
    box-sizing: border-box;
    display: block;
    padding: 0 1.5rem;
    text-indent: 0;

    & + * {
      margin-top: 1rem;
    }

    & + .embed {
      margin-top: 0;
    }
  }

  .footnote {
    font-size: 0.9rem;
    padding: 0.5rem 0;
    color: var(--muted-text-color);
    text-indent: 0;
  }

  .footnotes {
    list-style-type: none;
    margin-left: 0;
    padding-left: 0;
    border-top: 1px solid var(--muted-text-color);
    padding-top: 1rem;
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

  .smallCaps {
    font-variant-caps: small-caps;
  }

  .graphicNovelLine, .graphicNovelThought, .graphicNovelSfx {
    text-indent: 0;

    .graphicNovelSpeaker {
      display: block;
      text-align: left;
      text-transform: uppercase;
      font-style: normal;
      font-weight: bold;
    }

    .graphicNovelText {
      display: block;
      box-sizing: border-box;
      padding-left: 2rem;

      &:before, &:after {
        content: none;
      }
    }
  }

  .graphicNovelThought {
    text-indent: 0;

    .graphicNovelSpeaker:after {
      content: ' (thinking)';
      font-weight: normal;
      text-transform: none;
      user-select: auto;
      font-size: 0.8rem;
    }

    .graphicNovelText {
      font-style: italic;
    }
  }

  hr, .graphicNovelPanelSplit {
    display: block;
    overflow: hidden;
    width: 50%;
    height: 0;
    margin: 1rem auto;
    border: 1px solid rgba(0, 0, 0, 0.2);
    text-indent: 0;
  }

  .reset {
    font-style: normal;
    font-weight: normal;
  }

  .result-paragraph {
    background: rgb(var(--chapter-highlight-color));
  }

  .flash-paragraph {
    animation: flash-paragraph 1s ease;
  }

  @keyframes flash-paragraph {
    0% {
      background: rgba(var(--chapter-highlight-color), 0);
    }

    25% {
      background: rgba(var(--chapter-highlight-color), 1);
    }

    75% {
      background: rgba(var(--chapter-highlight-color), 1);
    }

    100% {
      background: rgba(var(--chapter-highlight-color), 0);
    }
  }
}
</style>
