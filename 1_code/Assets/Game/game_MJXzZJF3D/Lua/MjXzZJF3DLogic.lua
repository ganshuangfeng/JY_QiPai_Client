ext_require_audio("Game.normal_mj_common.Lua.audio_mj_config","mj")
--MatchLogic

package.loaded["Game.game_MJXzZJF3D.Lua.MjXzZJF3DModel"] = nil
require "Game.game_MJXzZJF3D.Lua.MjXzZJF3DModel"

package.loaded["Game.normal_mj_common.Lua.normal_majiang_lib"] = nil
normal_majiang = require "Game.normal_mj_common.Lua.normal_majiang_lib"

package.loaded["Game.normal_mj_common.Lua.MjAnimation"] = nil
require "Game.normal_mj_common.Lua.MjAnimation"

package.loaded["Game.normal_mj_common.Lua.MJParticleManager"] = nil
require "Game.normal_mj_common.Lua.MJParticleManager"

package.loaded["Game.game_MJXzZJF3D.Lua.MjXzZJFHallPanel3D"] = nil
require "Game.game_MJXzZJF3D.Lua.MjXzZJFHallPanel3D"

package.loaded["Game.game_MJXzZJF3D.Lua.MjXzZJFGamePanel3D"] = nil
require "Game.game_MJXzZJF3D.Lua.MjXzZJFGamePanel3D"

package.loaded["Game.normal_mj_common.Lua.MjMyShouPaiManger3D"] = nil
require "Game.normal_mj_common.Lua.MjMyShouPaiManger3D"

package.loaded["Game.normal_mj_common.Lua.MjShouPaiManger3D"] = nil
require "Game.normal_mj_common.Lua.MjShouPaiManger3D"

package.loaded["Game.game_MJXzZJF3D.Lua.MjXzZJFClearing3D"] = nil
require "Game.game_MJXzZJF3D.Lua.MjXzZJFClearing3D"

package.loaded["Game.normal_mj_common.Lua.MjCard"] = nil
require "Game.normal_mj_common.Lua.MjCard"

package.loaded["Game.normal_mj_common.Lua.MjCard3D"] = nil
require "Game.normal_mj_common.Lua.MjCard3D"

package.loaded["Game.game_MJXzZJF3D.Lua.MjXzZJFPlayerManger3D"] = nil
require "Game.game_MJXzZJF3D.Lua.MjXzZJFPlayerManger3D"

package.loaded["Game.game_MJXzZJF3D.Lua.MjXzZJFPairdesk3D"] = nil
require "Game.game_MJXzZJF3D.Lua.MjXzZJFPairdesk3D"

package.loaded["Game.game_MJXzZJF3D.Lua.MjXzZJFGangsRect3D"] = nil
require "Game.game_MJXzZJF3D.Lua.MjXzZJFGangsRect3D"

package.loaded["Game.normal_mj_common.Lua.MjPgManager3D"] = nil
require "Game.normal_mj_common.Lua.MjPgManager3D"

package.loaded["Game.normal_mj_common.Lua.MjYiChuPaiManager3D"] = nil
require "Game.normal_mj_common.Lua.MjYiChuPaiManager3D"

package.loaded["Game.CommonPrefab.Lua.GameVoiceLogic"] = nil
require "Game.CommonPrefab.Lua.GameVoiceLogic"

package.loaded["Game.normal_mj_common.Lua.MjHelpPanel"] = nil
require "Game.normal_mj_common.Lua.MjHelpPanel"

package.loaded["Game.CommonPrefab.Lua.GameSpeedyLogic"] = nil
require "Game.CommonPrefab.Lua.GameSpeedyLogic"

package.loaded["Game.CommonPrefab.Lua.GameAnimChatLogic"] = nil
require "Game.CommonPrefab.Lua.GameAnimChatLogic"
package.loaded["Game.CommonPrefab.Lua.PlayerInfoPanel"] = nil
require "Game.CommonPrefab.Lua.PlayerInfoPanel"

package.loaded["Game.normal_mj_common.Lua.MjDeskCenterManager3D"] = nil
require "Game.normal_mj_common.Lua.MjDeskCenterManager3D"

package.loaded["Game.game_MJXzZJF3D.Lua.MjxzZJFChangePrefab"] = nil
require "Game.game_MJXzZJF3D.Lua.MjxzZJFChangePrefab"

package.loaded["Game.game_MJXzZJF3D.Lua.MjxzZJFRuleChangeNoticePrefab"] = nil
require "Game.game_MJXzZJF3D.Lua.MjxzZJFRuleChangeNoticePrefab"

MjXzFKLogic={}

local panelNameMap =
{
	game = "MjXzZJFGamePanel3D",
}
MjXzFKLogic.panelNameMap = panelNameMap
local cur_loc
local cur_panel 

local this 
local updateDt=1
-- Logic ??? Update
local update
--????????????????????????
local req_sign_num_inval=3
local req_sign_num_count=0
--?????????????????????
local lister

local is_allow_forward=false
--view???????????????
local viewLister={}
local have_Jh
--???????????????
local jh_name="MjXzFK_fg_jh"
MjXzFKLogic.game_type = {
	nor_mj_xzdd = "nor_mj_xzdd",               ---- ????????????
	nor_mj_xzdd_er_7 = "nor_mj_xzdd_er_7",     ---- ??????7???????????????
	nor_mj_xzdd_er_13 = "nor_mj_xzdd_er_13",     ---- ??????13???????????????
}
--????????????????????????????????????????????????????????????????????????????????????
local function MakeLister()
    lister = {}
    --response
    lister["model_nor_mj_xzdd_signup_response"] = this.on_nor_mj_xzdd_signup_response
    lister["model_nor_mj_xzdd_cancel_signup_response"] = this.on_nor_mj_xzdd_cancel_signup_response
	lister["model_nor_mj_xzdd_auto_cancel_signup_msg"]=this.on_nor_mj_xzdd_auto_cancel_signup_msg

    lister["model_nor_mj_xzdd_statusNo_error_msg"] = this.on_nor_mj_xzdd_status_error_msg
    lister["model_nor_mj_xzdd_status_info"] = this.on_nor_mj_xzdd_status_info
    lister["model_friendgame_all_info"] = this.on_friendgame_all_info

    lister["ReConnecteServerSucceed"] = this.on_reconnect_msg
    lister["DisconnectServerConnect"] = this.on_network_error_msg

    lister["EnterForeGround"] = this.on_backgroundReturn_msg
    lister["EnterBackGround"] = this.on_background_msg
end

local function AddMsgListener(lister)
    for proto_name,func in pairs(lister) do
        Event.AddListener(proto_name, func)
    end
end

local function RemoveMsgListener(lister)
    for proto_name,func in pairs(lister) do
        Event.RemoveListener(proto_name, func)
    end
end

local function ViewMsgRegister(registerName)
	if registerName then
		if viewLister and viewLister[registerName] and is_allow_forward then
			AddMsgListener(viewLister[registerName])
		end
	else
		if viewLister and is_allow_forward then
			for k,lister in pairs(viewLister) do
				AddMsgListener(lister)
			end
		end
	end
end
local function cancelViewMsgRegister(registerName)
	if  registerName then
		if viewLister and viewLister[registerName] then 
			RemoveMsgListener(viewLister[registerName])
		end 
	else
		if viewLister then
			for k,lister in pairs(viewLister) do
				RemoveMsgListener(lister)
			end
		end
	end
	DOTweenManager.KillAllStopTween()
end

local function clearAllViewMsgRegister()
	cancelViewMsgRegister()
	viewLister={}
end

function MjXzFKLogic.setViewMsgRegister(lister,registerName)
	--????????????????????????
	if not registerName or viewLister[registerName] then
		return false
	end
	viewLister[registerName]=lister
	ViewMsgRegister(registerName)
end
function MjXzFKLogic.clearViewMsgRegister(registerName)
	if not registerName then 
		return false
	end
	cancelViewMsgRegister(registerName)
	viewLister[registerName]=nil
end
function MjXzFKLogic.refresh_panel()
	if cur_panel then
		cur_panel.instance:MyRefresh()
	end
end
function MjXzFKLogic.change_panel(panelName)
	if have_Jh then
		FullSceneJH.RemoveByTag(have_Jh)
		have_Jh=nil
	end
	if cur_panel then
		if cur_panel.name==panelName then
			cur_panel.instance:MyRefresh()
		else
			DOTweenManager.KillAllStopTween()
			cur_panel.instance:MyClose()
			cur_panel=nil
		end
	end
	if not cur_panel then
		if panelName==panelNameMap.game then
			cur_panel={name=panelName,instance=MjXzZJFGamePanel3D.Create()}
		else
			print("<color=red>Error: ????????????</color>")
		end
	end
	--cur_panel=MatchPanel.Show(load_callback)
end

-- ????????????
function MjXzFKLogic.on_nor_mj_xzdd_signup_response(result)
	MjXzFKLogic.change_panel(panelNameMap.game)
end
-- ????????????
function MjXzFKLogic.on_nor_mj_xzdd_cancel_signup_response(result)
	MainLogic.GotoScene("game_Hall")
end

--??????????????????????????????????????????????????????????????????????????????????????????
function MjXzFKLogic.on_nor_mj_xzdd_auto_cancel_signup_msg(result)
	MainLogic.GotoScene("game_Hall")
end

--?????? ??????????????????????????????
function MjXzFKLogic.on_friendgame_all_info()
	--????????????
	MjXzFKModel.data.limitDealMsg=nil
	
	MjXzFKLogic.change_panel(panelNameMap.game)
	is_allow_forward=true
	--????????????
	ViewMsgRegister()
end
--??????????????????
function MjXzFKLogic.on_nor_mj_xzdd_status_info()

	--if  ???????????????????????? then
			--?????????????????????????????????panel
	--else 
			--??????????????????????????????????????????



end

local function SendRequestAllInfo()
	--??????????????????  ??????????????????????????????
	MjXzFKModel.data.limitDealMsg={zijianfang_all_info=true}
	Network.SendRequest("zijianfang_req_info_by_send", {type="all"}, function (data)
		dump(data, "<color=red>zijianfang_req_info_by_send</color>")
	end)
end
--??????????????????**************
--??????????????????
function MjXzFKLogic.on_nor_mj_xzdd_status_error_msg()
	print("<color=red>??????????????????</color>")
	if not have_Jh then
		have_Jh=jh_name
		FullSceneJH.Create("??????????????????",have_Jh)
	end
	--??????view model
	cancelViewMsgRegister()
	SendRequestAllInfo()
end
--???????????????????????????
function MjXzFKLogic.on_backgroundReturn_msg()
	if not have_Jh then
		have_Jh=jh_name
		FullSceneJH.Create("??????????????????",have_Jh)
	end
	cancelViewMsgRegister()
	SendRequestAllInfo()
end
--??????????????????
function MjXzFKLogic.on_background_msg()
	DOTweenManager.CloseAllSequence()
	cancelViewMsgRegister()
end
--????????????????????????
function MjXzFKLogic.on_network_error_msg()
	cancelViewMsgRegister()
end
--??????????????????????????????
function MjXzFKLogic.on_network_repair_msg()
	
end
--?????????????????????
function MjXzFKLogic.on_network_poor_msg()
	
end
--????????????????????????
function MjXzFKLogic.on_reconnect_msg()
	--??????ALL??????
	print("<color=red>????????????????????????</color>")
	if not have_Jh then
		have_Jh=jh_name
		FullSceneJH.Create("??????????????????",have_Jh)
	end
	SendRequestAllInfo()
end
--??????????????????**************

function MjXzFKLogic.Update()

end
--?????????
function MjXzFKLogic.Init()
	GPSPanel.send_gps_info()
	this=MjXzFKLogic
	--?????????model
	local model = MjXzFKModel.Init()
	GameVoiceLogic.Init(model)

	GameSpeedyLogic.Init(model)
	GameAnimChatLogic.Init(model)

	MakeLister()
	AddMsgListener(lister)
	update=Timer.New(MjXzFKLogic.Update,updateDt,-1)
	update:Start()
	--??????ALL??????
	SendRequestAllInfo()
	MjXzFKLogic.change_panel(panelNameMap.game)
end
function MjXzFKLogic.Exit()

	this=nil
	update:Stop()
	update=nil
	if cur_panel then
		cur_panel.instance:MyExit()
	end
	cur_panel = nil

	GameSpeedyLogic.Exit()
	GameAnimChatLogic.Exit()

	RemoveMsgListener(lister)
	clearAllViewMsgRegister()
	MjXzFKModel.Exit()
	GameVoiceLogic.Exit()
end

return MjXzFKLogic














