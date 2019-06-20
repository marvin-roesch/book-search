<template>
<div class="login">
  <LoginForm @first-login="showFirstLoginForm = true" v-if="!showFirstLoginForm"></LoginForm>
  <FirstLoginForm v-else></FirstLoginForm>
</div>
</template>

<script>
import LoginForm from '@/components/LoginForm.vue';
import FirstLoginForm from '@/components/FirstLoginForm.vue';

export default {
  name: 'login',
  components: { FirstLoginForm, LoginForm },
  data() {
    return {
      showFirstLoginForm: false,
    };
  },
};
</script>

<style scoped lang="scss">
.login {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100vh;
}
</style>
