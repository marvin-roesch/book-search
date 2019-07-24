import Vue from 'vue';
import Vuex from 'vuex';
import api from '@/api';
import auth from '@/store/auth';
import notifications from '@/store/notifications';

Vue.use(Vuex);

function prepareSeries(path, series, seriesFilter, bookFilter) {
  series.books.forEach((b) => {
    Vue.set(
      b,
      'selected',
      (seriesFilter === null && bookFilter === null)
      || bookFilter === null
      || seriesFilter.some(f => path.match(f))
      || bookFilter.includes(b.id),
    );
  });
  series.children.forEach(s => prepareSeries(`${path}\\${s.name}`, s, seriesFilter, bookFilter));
}

const cachedSeries = window.localStorage.getItem('series');
const initialState = {
  darkMode: window.localStorage.getItem('darkMode') === 'true',
  series: !cachedSeries ? [] : JSON.parse(cachedSeries),
};

const mutations = {
  setDarkMode(state, enabled) {
    state.darkMode = enabled;
    window.localStorage.setItem('darkMode', enabled ? 'true' : 'false');
  },
  setSeries(state, series) {
    state.series = series;
    window.localStorage.setItem('series', JSON.stringify(series));
  },
  applySeriesFilter(state, { seriesFilter, bookFilter }) {
    if (state.series === null) {
      return;
    }
    state.series.forEach(s => prepareSeries(s.name, s, seriesFilter, bookFilter));
    window.localStorage.setItem('series', JSON.stringify(state.series));
  },
};

const actions = {
  async refreshSeries({ commit }, filter) {
    const { data: allSeries } = await api.get('/books/series');
    commit('setSeries', allSeries);
    commit('applySeriesFilter', filter);
  },
};

export default new Vuex.Store({
  state: initialState,
  mutations,
  actions,
  modules: {
    auth,
    notifications,
  },
});
