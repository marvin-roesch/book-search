<template>
<div class="book-wizard">
  <transition name="slide">
    <router-view></router-view>
  </transition>
</div>
</template>

<script>
export default {
  name: 'BookWizard',
};
</script>

<style lang="scss">
.book-wizard {
  padding: 2rem;
  background: var(--section-bg);
  border-radius: 3px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 0.75rem 1rem rgba(0, 0, 0, 0.1);
  width: 50vw;
  max-width: $max-content-width;
  display: flex;
  flex-direction: column;
  position: relative;
  max-height: 80vh;
  margin: 0 auto;
  box-sizing: border-box;

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
