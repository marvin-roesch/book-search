<template>
<Card class="book-wizard">
  <transition name="slide">
    <router-view></router-view>
  </transition>
</Card>
</template>

<script>
import Card from '@/components/Card.vue';

export default {
  name: 'BookWizard',
  components: { Card },
};
</script>

<style lang="scss">
.book-wizard {
  width: 50vw;
  max-width: $max-content-width;
  display: flex;
  flex-direction: column;
  position: relative;
  max-height: 80vh;
  margin: 0 auto;

  @media (max-width: $max-content-width) {
    width: 100%;
  }

  h2 {
    font-weight: 400;
    margin: 0 0 0.5rem;
  }
}

.button-bar {
  margin-top: 1rem;
  display: flex;

  *:last-child {
     margin-left: auto;
  }
}

.slide-enter-active, .slide-leave-active {
  transition: opacity .2s, max-height .2s;
  max-height: 80vh;
}

.slide-enter, .slide-leave-to {
  opacity: 0;
  max-height: 0;
}
</style>
