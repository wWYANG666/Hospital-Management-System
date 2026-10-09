# 上传到公开 GitHub 仓库

源码 ZIP 不包含 `.git`、本地凭据、用户头像、业务数据备份、日志或测试截图。若直接使用克隆的现有仓库，则按文末的后续更新步骤操作，不需要再次 `git init`。

## 1. 创建空仓库

在 GitHub 点击 New repository，填写仓库名并选择 Public。不要添加 README、.gitignore 或 License，以便直接推送本地版本。

## 2. 初始化并检查

在发行目录根目录运行：

```powershell
node scripts/check-public-release.mjs --strict
git init -b main
git config user.name "YOUR_GITHUB_USERNAME"
git config user.email "YOUR_GITHUB_NOREPLY_EMAIL"
git add .
git update-index --chmod=+x mvnw
git diff --cached --check
git diff --cached --stat
git commit -m "Initial public release"
```

`YOUR_GITHUB_NOREPLY_EMAIL` 请替换为 GitHub Settings -> Emails 中显示的 noreply 地址，以免提交元数据公开私人邮箱。这里的 `git config` 仅设置当前仓库。

如修改了本地配置，先确认该文件被忽略：

```powershell
git check-ignore config/application-local.properties
```

## 3. 关联远程仓库并推送

```powershell
git remote add origin https://github.com/YOUR_GITHUB_USERNAME/YOUR_REPOSITORY.git
git remote -v
git push -u origin main
```

使用 Git Credential Manager 的浏览器登录，或在 Git 提示密码时使用 Personal Access Token。不要把 Token 写进 URL、README 或脚本。

## 4. 检查页面

打开 GitHub 仓库，确认 main 分支的 README 正常显示，且目录中没有凭据、本地配置、用户文件和日志。将 README 的克隆 URL 改为真实仓库地址，完成仓库描述与主题标签。

当前未选择开源许可证；需要授予他人复用许可时，可自行补充 LICENSE。

后续更新：

```powershell
node scripts/check-public-release.mjs
git add .
git diff --cached --check
git commit -m "Describe your change"
git push
```

参考：[GitHub 官方上传说明](https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github)、[认证说明](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/about-authentication-to-github)。
