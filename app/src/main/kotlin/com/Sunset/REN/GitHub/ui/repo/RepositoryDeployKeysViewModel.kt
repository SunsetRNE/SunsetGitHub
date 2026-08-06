package com.Sunset.REN.GitHub.ui.repo

import android.app.Application
import androidx.lifecycle.*
import com.Sunset.REN.GitHub.data.github.html.*
import com.Sunset.REN.GitHub.data.local.EncryptedSharedPreferencesTokenStore
import com.Sunset.REN.GitHub.data.local.SharedPreferencesCurrentAccountStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RepositoryDeployKeysUiState(val owner:String="", val repo:String="", val isLoading:Boolean=false, val isSaving:Boolean=false, val errorMessage:String?=null, val pendingMessage:String?=null, val snapshot:RepositoryDeployKeysSnapshot?=null) { val isInitialLoad get()=isLoading&&snapshot==null&&errorMessage==null }

class RepositoryDeployKeysViewModel(app: Application): AndroidViewModel(app) {
    private val accountStore=SharedPreferencesCurrentAccountStore(app); private val tokenStore=EncryptedSharedPreferencesTokenStore(app)
    private val _state=MutableLiveData(RepositoryDeployKeysUiState()); val state:LiveData<RepositoryDeployKeysUiState> = _state
    private var token=""; private var prepared=false
    fun prepare(owner:String, repo:String){ if(prepared)return; prepared=true; _state.value=RepositoryDeployKeysUiState(owner,repo); refresh() }
    fun refresh(){ val s=_state.value?:return; if(s.owner.isBlank()||s.repo.isBlank())return; viewModelScope.launch{ _state.value=s.copy(isLoading=true,isSaving=false,errorMessage=null,pendingMessage=null); val t=token()?:run{_state.value=s.copy(isLoading=false,errorMessage="当前账号未登录或令牌已失效。");return@launch}; val r=withContext(Dispatchers.IO){ GitHubRepositoryDeployKeysGateway(t).loadDeployKeys(s.owner,s.repo)}; _state.value=r.toState((_state.value?:s).copy(isLoading=false)) } }
    fun add(title:String,key:String,readOnly:Boolean){ val s=_state.value?:return; if(title.trim().isBlank()||key.trim().isBlank()){_state.value=s.copy(errorMessage="请输入标题和公钥。");return}; mutate("部署密钥已添加。") { g,st -> g.addDeployKey(st.owner,st.repo,title.trim(),key.trim(),readOnly) } }
    fun delete(item:RepositoryDeployKeyItem){ mutate("部署密钥已删除。") { g,s -> g.deleteDeployKey(s.owner,s.repo,item.id) } }
    private fun mutate(msg:String, block:(GitHubRepositoryDeployKeysGateway,RepositoryDeployKeysUiState)->GitHubHtmlParseResult<*>){ val s=_state.value?:return; if(s.snapshot?.canAdmin!=true){_state.value=s.copy(errorMessage="当前账号没有管理员权限。");return}; viewModelScope.launch{ _state.value=s.copy(isSaving=true,errorMessage=null,pendingMessage="正在提交……"); val t=token()?:run{_state.value=s.copy(isSaving=false,errorMessage="当前账号未登录或令牌已失效。",pendingMessage=null);return@launch}; val r=withContext(Dispatchers.IO){block(GitHubRepositoryDeployKeysGateway(t),s)}; when(r){ is GitHubHtmlParseResult.Success->{_state.value=(_state.value?:s).copy(isSaving=false,pendingMessage=msg,errorMessage=null); refresh()}; is GitHubHtmlParseResult.AccessDenied->_state.value=s.copy(isSaving=false,errorMessage=r.message,pendingMessage=null); is GitHubHtmlParseResult.NotFound->_state.value=s.copy(isSaving=false,errorMessage=r.message,pendingMessage=null); is GitHubHtmlParseResult.ParseError->_state.value=s.copy(isSaving=false,errorMessage=r.message,pendingMessage=null)} } }
    private fun GitHubHtmlParseResult<RepositoryDeployKeysSnapshot>.toState(base:RepositoryDeployKeysUiState)=when(this){ is GitHubHtmlParseResult.Success->base.copy(snapshot=value,errorMessage=null); is GitHubHtmlParseResult.AccessDenied->base.copy(errorMessage=message); is GitHubHtmlParseResult.NotFound->base.copy(errorMessage=message); is GitHubHtmlParseResult.ParseError->base.copy(errorMessage=message)}
    private suspend fun token():String?{ if(token.isNotBlank())return token; val a=withContext(Dispatchers.IO){accountStore.getCurrentAccount()}?:return null; token=withContext(Dispatchers.IO){tokenStore.getAccessToken(a.id)}?.takeIf{it.isNotBlank()}?:return null; return token }
}