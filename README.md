# commit 내역

## 2023-05-25
### 레코드 생성 fab 추가
우측 상단 menu를 통해 Record를 생성하는 대신에<br>
<b>fab</b>을 통해 Record를 생성하도록 변경 

### 정렬기능 추가
우측 상단 menu를 통해 정렬 기준을 설정하는 radio dialog를 출력.<br>
정렬 기준은 <b>sharedPreference</b>로 기록하여 앱 종료시에도 유지.

### 날짜 설정 기능
날짜 설정 버튼을 누르면 <b>DialogFragment</b>로 이동.<br>
Record의 날짜정보를 기반으로 <b>custom dialog를 생성</b>한다.<br>
custom dialog는 spinner 형태의 <b>datePicker와 timePicker를 결합</b>하여 사용자로부터 날짜/시간을 입력 받음.<br>

<b>bottomSheetDialog</b> 적용

### 일괄삭제 기능 추가
좌측 상단의 네비게이션의 통해 데이터 관리 탭에 들어가서 데이터 일괄삭제 할 수 있도록 설계
