<template>
  <div class="home">
    <el-card class="hero">
      <h1>志愿服务管理平台</h1>
      <p>活动发布 · 在线报名 · 签到签退 · 工时核算 · 服务档案 · 证明生成 · 统计可视化</p>
      <el-alert
        title="当前为项目骨架阶段：前端页面已可运行，后端模块正在开发中。"
        type="info"
        :closable="false"
        show-icon
        style="margin-top: 16px"
      />
    </el-card>

    <el-row :gutter="16" class="modules">
      <el-col v-for="m in modules" :key="m.name" :span="8">
        <el-card shadow="hover" class="module-card">
          <h3>{{ m.name }}</h3>
          <p>{{ m.desc }}</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
const modules = [
  { name: '活动管理', desc: '活动发布、展示与状态流转' },
  { name: '报名管理', desc: '在线报名、审核与名单导出' },
  { name: '签到签退', desc: '签到记录、服务时长自动计算' },
  { name: '工时核算', desc: '时长统计汇总，规则可回溯' },
  { name: '档案与证明', desc: '参与历史、累计工时、证明生成' },
  { name: '系统管理', desc: '用户、活动类型、公告发布' }
]
</script>

<style scoped>
.home { max-width: 1080px; margin: 0 auto; padding: 32px 16px; }
.hero { text-align: center; margin-bottom: 24px; }
.hero h1 { font-size: 28px; color: #1f2d3d; }
.hero p { margin-top: 8px; color: #64748b; }
.modules { row-gap: 16px; }
.module-card { text-align: center; }
.module-card h3 { font-size: 18px; color: #1e3a8a; }
.module-card p { margin-top: 6px; color: #64748b; font-size: 14px; }
</style>
