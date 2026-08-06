package com.Sunset.REN.GitHub.ui.repo

import android.app.Application
import androidx.lifecycle.*
import com.Sunset.REN.GitHub.data.github.html.*
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RepositoryWebhooksUiState(val owner:String="", val repo:String="", val isLoading:Boolean=false, val isSaving:Boolean=false, val errorMessage:String?=null, val pendingMessage:String?=null, val snapshot:RepositoryWebhooksSnapshot?=null) { val isInitialLoad get()=isLoading&&snapshot==null&&errorMessage==null }

class RepositoryWebhooksViewModel(app: Application): AndroidViewModel(app) {
    private val accountStore=SharedPreferencesCurrentAccountStore(app); private val tokenStore=EncryptedSharedPreferencesTokenStore(app)
    private val _state=MutableLiveData(RepositoryWebhooksUiState()); val state:LiveData<RepositoryWebhooksUiState> = _state
    private var token=""; private var prepared=false
    fun prepare(owner:String, repo:String){ if(prepared)return; prepared=true; _state.value=RepositoryWebhooksUiState(owner,repo); refresh() }
    fun refresh(){ val s=_state.value?:return; if(s.owner.isBlank()||s.repo.isBlank())return; viewModelScope.launch{ _state.value=s.copy(isLoading=true,isSaving=false,errorMessage=null,pendingMessage=null); val t=token()?:run{_state.value=s.copy(isLoading=false,errorMessage="当前账号未登录或令牌已失效。");return@launch}; val r=withContext(Dispatchers.IO){ GitHubRepositoryWebhooksGateway(t).loadWebhooks(s.owner,s.repo)}; _state.value=r.toState((_state.value?:s).copy(isLoading=false)) } }
    fun create(url:String, secret:String, events:String){ mutate("Webhook 已创建。") { g,s -> g.createWebhook(s.owner,s.repo, RepositoryWebhookUpsertRequest(url=url.trim(), secret=secret.trim().takeIf{it.isNotBlank()}, events=events.split(',','\n').map{it.trim()}.filter{it.isNotBlank()})) } }
    fun ping(item:RepositoryWebhookItem){ mutate("Webhook Ping 已发送。") { g,s -> g.pingWebhook(s.owner,s.repo,item.id) } }
    fun delete(item:RepositoryWebhookItem){ mutate("Webhook 已删除。") { g,s -> g.deleteWebhook(s.owner,s.repo,item.id) } }
    private fun mutate(msg:String, block:(GitHubRepositoryWebhooksGateway,RepositoryWebhooksUiState)->GitHubHtmlParseResult<*>){ val s=_state.value?:return; if(s.snapshot?.canAdmin!=true){_state.value=s.copy(errorMessage="当前账号没有管理员权限。");return}; viewModelScope.launch{ _state.value=s.copy(isSaving=true,errorMessage=null,pendingMessage="正在提交……"); val t=token()?:run{_state.value=s.copy(isSaving=false,errorMessage="当前账号未登录或令牌已失效。",pendingMessage=null);return@launch}; val r=withContext(Dispatchers.IO){block(GitHubRepositoryWebhooksGateway(t),s)}; when(r){ is GitHubHtmlParseResult.Success->{_state.value=(_state.value?:s).copy(isSaving=false,pendingMessage=msg,errorMessage=null); refresh()}; is GitHubHtmlParseResult.AccessDenied->_state.value=s.copy(isSaving=false,errorMessage=r.message,pendingMessage=null); is GitHubHtmlParseResult.NotFound->_state.value=s.copy(isSaving=false,errorMessage=r.message,pendingMessage=null); is GitHubHtmlParseResult.ParseError->_state.value=s.copy(isSaving=false,errorMessage=r.message,pendingMessage=null)} } }
    private fun GitHubHtmlParseResult<RepositoryWebhooksSnapshot>.toState(base:RepositoryWebhooksUiState)=when(this){ is GitHubHtmlParseResult.Success->base.copy(snapshot=value,errorMessage=null); is GitHubHtmlParseResult.AccessDenied->base.copy(errorMessage=message); is GitHubHtmlParseResult.NotFound->base.copy(errorMessage=message); is GitHubHtmlParseResult.ParseError->base.copy(errorMessage=message)}
    private suspend fun token():String?{ if(token.isNotBlank())return token; val a=withContext(Dispatchers.IO){accountStore.getCurrentAccount()}?:return null; token=withContext(Dispatchers.IO){tokenStore.getAccessToken(a.id)}?.takeIf{it.isNotBlank()}?:return null; return token }
}