<template>
<Fullscreen>
  <LoginForm @first-login="showFirstLoginForm = true" v-if="!showFirstLoginForm"></LoginForm>
  <FirstLoginForm v-else></FirstLoginForm>
</Fullscreen>
</template>

<script>
import LoginForm from '@/components/LoginForm.vue';
import FirstLoginForm from '@/components/FirstLoginForm.vue';
import Fullscreen from '@/views/Fullscreen.vue';

export default {
  name: 'login',
  components: { Fullscreen, FirstLoginForm, LoginForm },
  data() {
    return {
      showFirstLoginForm: false,
    };
  },
};
</script>

<style lang="scss">
.fullscreen {
  padding-top: 1rem;
}
</style>
