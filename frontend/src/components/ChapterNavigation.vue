<template>
<div :class="fixed ? 'chapter-navigation-fixed' : 'chapter-navigation'">
  <router-link
    class="chapter-navigation-prev"
    :to="{ name: 'chapter', params: { id: prev } }"
    title="Previous chapter"
    v-if="prev !== null">
    <ChevronLeftIcon :width="fixed ? 48 : 24" :height="fixed ? 48 : 24">
    </ChevronLeftIcon>
    <template v-if="!fixed">Previous</template>
  </router-link>
  <router-link
    class="chapter-navigation-next"
    :to="{ name: 'chapter', params: { id: next } }"
    title="Next chapter"
    v-if="next !== null">
    <template v-if="!fixed">Next</template>
    <ChevronRightIcon :width="fixed ? 48 : 24" :height="fixed ? 48 : 24">
    </ChevronRightIcon>
  </router-link>
</div>
</template>

<script>
import { ChevronLeftIcon, ChevronRightIcon } from 'vue-feather-icons';

export default {
  name: 'ChapterNavigation',
  components: {
    ChevronRightIcon,
    ChevronLeftIcon,
  },
  props: {
    fixed: Boolean,
    prev: String,
    next: String,
  },
};
</script>

<style scoped lang="scss">
.chapter-navigation {
  display: none;
  justify-content: center;
  margin: 0.5rem auto;

  a {
    display: flex;
    align-items: center;
  }

  .chapter-navigation-prev {
    margin-right: auto;
  }

  .chapter-navigation-next {
    margin-left: auto;
  }

  &-fixed {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    pointer-events: none;

    .chapter-navigation-prev {
      margin-right: 25%;

      &:only-child {
        margin-right: 50%;
        padding-right: 3rem;
      }
    }

    .chapter-navigation-next {
      margin-left: 25%;

      &:only-child {
        margin-left: 50%;
        padding-left: 3rem;
      }
    }

    a {
      pointer-events: auto;
    }

    @media (max-width: 1200px) {
      .chapter-navigation-prev {
        margin-right: 35%;

        &:only-child {
          margin-right: 70%;
        }
      }

      .chapter-navigation-next {
        margin-left: 35%;

        &:only-child {
          margin-left: 70%;
        }
      }
    }

    @media (max-width: $max-content-width) {
      display: none;
    }
  }

  @media (max-width: $max-content-width) {
    display: flex;
  }
}
</style>
