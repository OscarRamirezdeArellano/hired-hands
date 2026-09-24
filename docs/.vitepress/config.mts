import { defineConfig } from 'vitepress'

const REPO = 'https://github.com/OscarRamirezdeArellano/hired-hands'

function barra(pre: string, t: Record<string, string>) {
  return [
    {
      text: t.start,
      items: [
        { text: t.intro, link: `${pre}/` },
        { text: t.hiring, link: `${pre}/hiring` },
        { text: t.orders, link: `${pre}/orders` },
      ],
    },
    {
      text: t.work,
      items: [
        { text: t.trades, link: `${pre}/trades` },
        { text: t.miner, link: `${pre}/miner` },
        { text: t.areas, link: `${pre}/work-areas` },
      ],
    },
    {
      text: t.more,
      items: [
        { text: t.chat, link: `${pre}/chat` },
        { text: t.config, link: `${pre}/configuration` },
        { text: t.recipes, link: `${pre}/recipes` },
        { text: t.faq, link: `${pre}/faq` },
      ],
    },
  ]
}

export default defineConfig({
  title: 'Hired Hands',
  cleanUrls: true,
  lastUpdated: false,
  head: [
    ['link', { rel: 'icon', type: 'image/png', href: '/favicon.png' }],
    ['meta', { property: 'og:image', content: '/img/01-hired-hands.webp' }],
  ],
  themeConfig: {
    logo: '/img/logo.png',
    socialLinks: [{ icon: 'github', link: REPO }],
    search: { provider: 'local' },
  },
  locales: {
    root: {
      label: 'English',
      lang: 'en',
      description: 'Hireable mercenaries and workers for Minecraft 26.2 (NeoForge).',
      themeConfig: {
        nav: [
          { text: 'Guide', link: '/hiring' },
          { text: 'Trades', link: '/trades' },
          { text: 'Configuration', link: '/configuration' },
          { text: 'Download', link: 'https://www.curseforge.com/minecraft/mc-mods/hired-hands' },
        ],
        sidebar: barra('', {
          start: 'Getting started', intro: 'Introduction', hiring: 'Hiring and contracts', orders: 'Orders and modes',
          work: 'Work', trades: 'Trades', miner: 'Miner', areas: 'Work areas',
          more: 'More', chat: 'Chat and AI', config: 'Configuration', recipes: 'Recipes', faq: 'FAQ',
        }),
        footer: { message: 'MIT License. Not an official Minecraft product.', copyright: 'Hired Hands by OscarWays' },
      },
    },
    es: {
      label: 'Español',
      lang: 'es',
      link: '/es/',
      description: 'Mercenarios y trabajadores contratables para Minecraft 26.2 (NeoForge).',
      themeConfig: {
        nav: [
          { text: 'Guía', link: '/es/hiring' },
          { text: 'Oficios', link: '/es/trades' },
          { text: 'Configuración', link: '/es/configuration' },
          { text: 'Descargar', link: 'https://www.curseforge.com/minecraft/mc-mods/hired-hands' },
        ],
        sidebar: barra('/es', {
          start: 'Primeros pasos', intro: 'Introducción', hiring: 'Contratar', orders: 'Órdenes y modos',
          work: 'Trabajo', trades: 'Oficios', miner: 'Minero', areas: 'Zonas de trabajo',
          more: 'Más', chat: 'Chat e IA', config: 'Configuración', recipes: 'Recetas', faq: 'Preguntas frecuentes',
        }),
        outline: { label: 'En esta página' },
        docFooter: { prev: 'Anterior', next: 'Siguiente' },
        darkModeSwitchLabel: 'Tema',
        sidebarMenuLabel: 'Menú',
        returnToTopLabel: 'Volver arriba',
        footer: { message: 'Licencia MIT. No es un producto oficial de Minecraft.', copyright: 'Hired Hands, de OscarWays' },
      },
    },
  },
})
