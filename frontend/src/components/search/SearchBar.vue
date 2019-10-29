<template>
<div :class="['search-bar', toolbar ? 'search-bar-toolbar' : undefined]">
  <div class="search-bar-icon">
    <SearchIcon></SearchIcon>
  </div>
  <input type="text" :value="query" @keydown.enter="search" ref="search">
  <div class="search-bar-help-icon">
    <HelpCircleIcon @click="showHelpDialog = !showHelpDialog" ref="help-icon"></HelpCircleIcon>
  </div>
  <transition name="fade-relative">
    <div class="search-bar-help-dialog" v-if="showHelpDialog">
      <transition name="fade-slide-up">
        <Card
          v-closable="{
            exclude: ['help-icon'],
            handler() { showHelpDialog = false; }
          }">
          <template slot="title">Help
          <XIcon @click="showHelpDialog = false"></XIcon>
          </template>
          <div class="search-bar-help-content">
            <p>
              By default, paragraphs containing at least one of every word from your query in any
              order will be displayed. Search results do not depend on casing.
            </p>
            <p>You have the following options to refine your search:</p>
            <ul>
              <li>
                <strong>Search in chapters:</strong>
                If you choose "Search in chapters" rather than "Search in paragraphs", the entire
                text of a chapter will be searched. You'll only see the chapter title as a
                result, but the chapter view will highlight all occurrences of your search terms
                within the chapter.
              </li>
              <li>
                <strong>Quoting:</strong>
                Putting terms in quotes will search for exactly the quoted phrase, disregarding
                punctuation. Only paragraphs containing every word from the phrase in consecutive
                order will be displayed.<br>
                Example: <code>"quote it"</code>
              </li>
              <li>
                <strong>Wildcards:</strong>
                Search is usually only performed on entire words. If you want to include results
                where a term only appears within another word, you can use wildcards.
                An asterisk (<code>*</code>) will match any number of characters in a word,
                while a question mark (<code>?</code>) will stand in as replacement for exactly
                one character.<br>
                Examples: <code>Kal*</code> matches all words starting with "Kal",
                <code>Kala?</code> matches "Kalad" and "Kalak", <code>*weaver</code> matches
                all words ending in "weaver"
              </li>
              <li>
                <strong>Case Sensitivity:</strong>
                Prefixing any term or quoted phrase with <code>text.cs:</code> will only yield
                results where the casing matches your query exactly.<br>
                Examples: <code>text.cs:Wit</code>, <code>text.cs:"invested art"</code>
              </li>
              <li>
                <strong>Boolean expressions and grouping:</strong>
                By putting <code>AND</code> or <code>OR</code> between terms you can search for
                paragraphs containing one of several alternatives. The operators <em>must</em>
                be written in capital letters. Leaving out an operator defaults to <code>AND</code>.
                If you want to group several expressions into subqueries, you can put them in
                parentheses.<br>
                Examples: <code>(shallan AND pattern) OR lightweaver</code>,
                <code>(melaan wayne) OR (wax* steris)</code>
              </li>
              <li>
                <strong>Negation:</strong> Individual terms or entire groups can be excluded
                from the search using <code>NOT</code>, <code>!</code> or <code>-</code> as a
                prefix. Note that only negated terms will lead to large result sets that may
                be cut off.<br>
                Examples: <code>vin -elend</code>, <code>silence !(shade OR silver)</code>
              </li>
            </ul>
          </div>
        </Card>
      </transition>
    </div>
  </transition>
</div>
</template>

<script>
import { HelpCircleIcon, SearchIcon, XIcon } from 'vue-feather-icons';
import Card from '@/components/Card.vue';

export default {
  name: 'SearchBar',
  components: { XIcon, Card, SearchIcon, HelpCircleIcon },
  props: {
    query: String,
    toolbar: Boolean,
    autoFocus: Boolean,
  },
  data() {
    return {
      showHelpDialog: false,
    };
  },
  mounted() {
    if (this.autoFocus) {
      this.$refs.search.focus();
    }
  },
  methods: {
    search(event) {
      this.$emit('search', event.target.value);
    },
  },
  beforeRouteUpdate(to, from, next) {
    this.showHelpDialog = false;
    next();
  },
};
</script>

<style lang="scss">
.search-bar {
  position: relative;
  width: 100%;
  max-width: $max-content-width;
  z-index: 2500;

  @media (max-width: $max-content-width) {
    width: 100%;
  }

  &-icon {
    display: flex;
    align-items: center;
    padding: 0 1rem;
    position: absolute;
    box-sizing: border-box;
    top: 0;
    left: 0;
    bottom: 0;
  }

  input {
    width: 100%;
    font-size: 1rem;
    color: var(--base-text-color);
    line-height: 1;
    box-shadow: rgba(0, 0, 0, 0.1) 0 0 0 1px, rgba(0, 0, 0, 0.1) 0 2px 4px;
    font-family: 'Nunito Sans', Helvetica, Arial, sans-serif;
    -webkit-appearance: none;
    padding: 1.5rem 3.5rem 1.5rem 56px;
    border: none;
    border-radius: 4px;
    outline: 0;
    box-sizing: border-box;
    background: var(--section-bg);
  }

  &-help-icon {
    display: flex;
    align-items: center;
    padding: 0 1rem;
    position: absolute;
    box-sizing: border-box;
    top: 0;
    right: 0;
    bottom: 0;

    .feather {
      cursor: pointer;
    }
  }

  &-toolbar {
    flex-grow: 1;
    width: 100%;

    input {
      padding: 0.75rem 3.5rem 0.75rem 56px;
      border: 1px solid rgba(0, 0, 0, 0.1);
      box-shadow: none;
    }
  }

  &-help-dialog {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.1);
    z-index: 4000;
    cursor: pointer;
    box-sizing: border-box;
    padding: 1rem;

    .card {
      cursor: initial;
      animation-name: slide-up;
      animation-timing-function: ease-in-out;
      animation-duration: 0.2s;
      max-width: $max-content-width;
      display: flex;
      flex-direction: column;
      align-items: stretch;
      max-height: 100%;

      h2 {
        display: flex;
        align-items: center;

        .feather {
          margin-left: auto;
          cursor: pointer;
        }
      }

      .search-bar-help-content {
        max-height: 100%;
        overflow-y: auto;
      }

      p {
        margin: 0;
        padding: 0.25rem 0;
      }

      ul {
        margin: 0;
        padding: 0;
        list-style-type: none;

        li {
          padding: 0.25rem 0;
        }
      }

      code {
        background: rgba(0, 0, 0, 0.05);
        padding: 0.125rem;
      }

      @media (max-width: 1200px) {
        max-width: 70%;
      }

      @media (max-width: $max-content-width) {
        max-width: 90%;
      }

      @media (max-width: 640px) {
        width: 100%;
        max-width: 100%;
      }
    }
  }
}

.shared-element-transition .search-bar-help-dialog {
  display: none !important;
}
</style>
