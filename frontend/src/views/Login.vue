<template>
<div class="login">
  <Form class="login-form" title="Login" @submit.prevent="login">
    <TextField
      name="username"
      v-model="username"
      placeholder="Username"
    >
      <template slot="icon">
      <UserIcon></UserIcon>
      </template>
    </TextField>
    <TextField
      type="password"
      name="password"
      v-model="password"
      placeholder="Password"
    >
      <template slot="icon">
      <LockIcon></LockIcon>
      </template>
    </TextField>
    <template slot="footer">
    <Button slim @click="login" :loading="verifying">Login</Button>
    </template>
  </Form>
</div>
</template>

<script>
import TextField from '@/components/TextField.vue';
import Button from '@/components/Button.vue';
import Form from '@/components/Form.vue';
import { LockIcon, UserIcon } from 'vue-feather-icons';

export default {
  name: 'login',
  components: { Form, Button, TextField, LockIcon, UserIcon },
  data() {
    return {
      username: '',
      password: '',
      verifying: false,
    };
  },
  methods: {
    async login() {
      const {
        username, password,
      } = this;
      this.verifying = true;
      try {
        await this.$store.dispatch('login', { username, password });
        this.$router.replace(this.$route.query.redirect || '/');
      } catch (error) {
        console.log(error);
      }
      this.verifying = false;
    },
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

  &-content {
    position: relative;

    h1 {
      position: absolute;
      bottom: 100%;
      width: 100%;
      text-align: center;
    }
  }
}
</style>
